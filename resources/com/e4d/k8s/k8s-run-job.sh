#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

logger=log_nothig

log_to_stdout() {
  echo "${@}"
}

log_to_file() {
  local file="${1}"
  shift
  echo "${@}" >> "${file}"
}

log_nothig() {
  return
}

info() {
  ${logger} INFO: "${@}"
}

random_token() {
  local number="$(printf '%+4x' $((RANDOM)) )"
  echo "${number// /0}"
}

from_seconds() {
  echo "${1:-1}"
}

from_minutes() {
  echo $((${1:-1} * 60))
}

from_hours() {
  echo $(from_minutes $((${1:-1} * 60)))
}

k8s() {
  kubectl "${KUBECONFIG:+--kubeconfig=${KUBECONFIG}}" "${CONTEXT:+--context=${CONTEXT}}" --request-timeout=10s "${@}"
}

apply() {
  local resource=$(echo "${1}" | k8s apply --filename=- --output=name)
  echo "${resource}"
}

job_log() {
  k8s logs job/"${1}"
}

job_pod() {
  local template="
    {{- if gt (len .items) 0 -}}
      {{- (index .items 0).metadata.name -}}
    {{- end -}}"
  k8s get pod --selector=job-name="${1}" --output="go-template=${template}"
}

wait_job_pod() {
  local job_name="${1}"
  local job_pod=$(job_pod "${job_name}")
  local max_spent=$((${2:-$(from_hours)} * 5))
  local sleep_interval=0.2
  local spent=0
  while [ ! "${job_pod}" -a ${spent} -lt ${max_spent} ]
  do
    sleep ${sleep_interval}
    spent=$((spent + 1))
    job_pod="$(job_pod "${job_name}")"
  done
  if [ ${spent} -eq ${max_spent} ]
  then
    exit 1
  fi
  echo "${job_pod}"
}

get_container_state() {
  local pod="${1}"
  local container="${2}"
  local kind="${3:-regular}"
  local statuses='containerStatuses'
  if [ "${kind}" = 'init' ]; then statuses='initContainerStatuses'; fi
  k8s get pod "${pod}" --output="go-template=\
    {{- range .status.${statuses} -}}
      {{- if eq \"${container}\" .name -}}
        {{- range \$index, \$sate := .state -}}
          {{- \$index -}}
        {{- end -}}
      {{- end -}}
    {{- end -}}
  "
}

get_init_container_state() {
  local pod="${1}"
  local container="${2}"
  get_container_state "${pod}" "${container}" init
}

wait_container_state() {
  local pod="${1}"
  local container="${2}"
  local state="${3}"
  local timeout=$((${4:-$(from_minutes)} * 5))
  local getter="${5:-get_container_state}"
  local sleep_interval=0.2
  local spent=0
  while [ ! "$(${getter} "${pod}" "${container}")" == "${state}" -a ${spent} -lt ${timeout} ]
  do
    sleep ${sleep_interval}
    spent=$((spent + 1))
  done
  if [ ${spent} -eq ${timeout} ]
  then
    exit 1
  fi
}

wait_init_container_state() {
  local pod="${1}"
  local container="${2}"
  local state="${3}"
  local timeout="${4}"
  wait_container_state "${pod}" "${container}" "${state}" "${timeout}" 'get_init_container_state'
}

get_nodes() {
  k8s get node --output="go-template={{ range .items }} {{ .metadata.name }} {{ end }}"
}

make_job_manifest() {
  local job_name="${1}"
  local node_name="${2}"
  local job_image="${3}"
  local sleep_interval=0.2
  local init_script="while [ ! -f /var/${job_name} ]; do sleep ${sleep_interval}; done"
  local job_script="cd /var/workspace; /var/${job_name}; touch /var/${job_name}-completed; while [ ! -f /var/${job_name}-released ]; do sleep ${sleep_interval}; done"
  echo "\
apiVersion: batch/v1
kind: Job
metadata:
  name: \"${job_name}\"
spec:
  template:
    metadata:
      labels:
        organization: e4d
        app: jenkins
        module: com.e4d.k8s.k8s-run-job
    spec:
      nodeName: \"${node_name}\"
      volumes:
      - name: docker-sock
        hostPath:
          path: /var/run/docker.sock
      - name: var
        emptyDir: {}
      - name: workspace
        emptyDir: {}
      initContainers:
      - name: initializer
        image: busybox
        command: [ \"sh\", \"-c\", \"${init_script}\" ]
        volumeMounts:
        - name: var
          mountPath: /var
        - name: workspace
          mountPath: /var/workspace
      containers:
      - name: worker
        image: "${job_image}"
        command: [ \"sh\", \"-c\", \"${job_script}\" ]
        volumeMounts:
          - name: docker-sock
            mountPath: /var/run/docker.sock
          - name: var
            mountPath: /var
          - name: workspace
            mountPath: /var/workspace
      restartPolicy: Never
"
}

run_node_job() {
  local node_name="${1}"
  shift
  local job_image="${1}"
  shift
  local job_script="${@}"
  local job_name="j$(random_token)$(random_token)"
  local job_script_file=$(mktemp)
  local sleep_interval=0.2

  if [ "${node_name}" == "@" ]; then node_name=""; fi

  info "run job on node ${node_name}"

  info 'write script file'
  echo "${job_script}" > "${job_script_file}"; chmod a+x "${job_script_file}"

  trap "{
    info 'clean up'
    rm -f "${job_script_file}"
    echo \"\$(job_log \"${job_name}\")\"
    k8s delete job "${job_name}" --output=name > /dev/null 
  }" RETURN INT TERM EXIT

  info 'apply job manifest'
  local job_manifest=$(make_job_manifest "${job_name}" "${node_name}" "${job_image}")
  local job_resource=$(apply "${job_manifest}")

  info 'wait pod'
  local job_pod=$(wait_job_pod "${job_name}" 10)

  info 'wait initializer start running'
  wait_init_container_state "${job_pod}" initializer running 10

  info 'copy job script to pod'
  k8s cp "${job_script_file}" "${job_pod}":/var/"${job_name}" --container=initializer

  info 'wait worker to start running'
  wait_container_state "${job_pod}" worker running 10

  info 'wait worker to compete its job'
  k8s exec "${job_pod}" --container=worker -- 'sh' '-c' "while [ ! -f /var/${job_name}-completed  ]; do sleep ${sleep_interval}; done"

  info 'copy all produced files from pod'
  k8s cp "${job_pod}":var/workspace ./ --container=worker

  info 'release worker'
  k8s exec "${job_pod}" --container=worker -- touch "/var/${job_name}-released"
}

run_job() {
  if [ "${#@}" -eq 0 ]
  then
    return
  fi

  local -a args=()
  local -a nodes=()
  local image="busybox"
  while [ ${#} -gt 0 ]
  do
    case "${1}" in
      --node=*)
        nodes+=("${1#*=}");;
      --all-nodes*)
        nodes=($(get_nodes));;
      --image=*)
        image="${1#*=}";;
      --log*)
        local file="${1#*=}";
        if [ "${file}" = '' -o "${file}" = "--log" ]; then logger="log_to_stdout";
        else logger="log_to_file ${file}";fi
        ;;
      *)
        args+=("${1#*=}");;
    esac
    shift
  done

  for node in "${nodes[@]:-"@"}"
  do
    run_node_job "${node}" "${image}" "${args[@]}" &
  done

  trap "wait" RETURN INT TERM EXIT
}

run_job "${@}"
