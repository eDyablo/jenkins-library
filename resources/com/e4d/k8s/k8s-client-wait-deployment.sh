#!/usr/bin/env bash

set -o errexit
set -o pipefail

trap "exit 2" TERM

check_interval=${CHECK_INTERVAL:-0.5}
completion_timeout=${COMPLETION_TIMEOUT:-300}
deployment="${DEPLOYMENT}"
namespace="${NAMESPACE:-default}"

no_value="<no value>"

while true; do
  case "$1" in
    --deployment ) readonly deployment="$2" ; shift 2 ;;
    --namespace ) readonly namespace="$2" ; shift 2 ;;
    --check-interval ) readonly check_interval="$2" ; shift 2 ;;
    --completion_timeout ) readonly completion_timeout="$2" ; shift 2 ;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

k8s() {
  kubectl "${KUBECONFIG:+--kubeconfig=${KUBECONFIG}}" "${CONTEXT:+--context=${CONTEXT}}" "${@}"
}

get_selectors() {
  get_deployment_go '{{ .spec.selector.matchLabels.app }}'
}

get_pod_images() {
  get_pods_go '{{ range .items }}{{"\n"}}Pod {{ .metadata.name }}{{""}} {{ range $index, $status := .status.containerStatuses }} {{ "\n\t" }}{{ $index }}. Container {{ $status.name }}{{ range $state, $statedetails := .state }} {{ $state }} {{ end }}{{ " " }}image {{ .image }}{{ end }}{{ "\n" }}{{ end }}'
}

get_generation() {
  get_deployment_go '{{.metadata.generation}}'
}

get_observed_generation() {
  get_deployment_go '{{.status.observedGeneration}}'
}

get_replicas() {
  get_deployment_go '{{.spec.replicas}}'
}

get_available_replicas() {
  local replicas=$(get_deployment_go '{{.status.availableReplicas}}')
  if [[ ${replicas} -ne ${no_value} ]]; then
    echo ${replicas}
  else
    echo 0
  fi
}

get_ready_replicas() {
  local replicas=$(get_deployment_go '{{.status.readyReplicas}}')
  if [[ ${replicas} != ${no_value} ]]; then
    echo ${replicas}
  else
    echo 0
  fi
}

get_updated_replicas() {
  get_deployment_go '{{.status.updatedReplicas}}'
}

get_deployment_go() {
  local readonly _go="$1"

  k8s get deployment "${deployment}" --namespace="${namespace}" --output "go-template=${_go}"
  if [ $? != 0 ]; then
    kill -s TERM $$
  fi
}

get_pods_go() {
  local readonly _go="$1"

  k8s get pods -l "app=$(get_selectors)" --namespace="${namespace}" --output "go-template=${_go}"
  if [ $? != 0 ]; then
    kill -s TERM $$
  fi
}

get_timestamp() {
  date --utc +"%s"
}

get_time_spent() {
  echo $(($(get_timestamp) - ${start_time}))
}

is_timeout_exceeded() {
  if [[ $(get_time_spent) -gt ${completion_timeout} ]]
  then
    return 0
  else
    return 1
  fi
}

get_deployment_new_replicaset() {
  local deployment_names=(${@})
  k8s --namespace=${namespace} describe deployment ${deployment_names[@]} | grep '^NewReplicaSet:\s*' | awk '{ print $2 }'
}

get_logs() {
  local resource_kind=pod
  local names
  while [ ${#} -gt 0 ]
  do
    case "${1}" in
      resource=*)
        resource_kind="${1#*=}";;
    *)
      names=(${names[@]} "${1#*=}");;
    esac
    shift
  done
  for name in ${names[@]}
  do
    echo "Logs for ${resource_kind}/${name}:"
    k8s --namespace=${namespace} logs "${resource_kind}/${name}"
  done
}

report_deployment_issues() {
  replica=$(k8s get rs -n "${namespace}" -o go-template="{{range .items}}{{\$rs := .}}{{range .metadata.ownerReferences}}{{if eq .name \"${deployment}\"}}{{if not \$rs.status.readyReplicas}}{{if \$rs.spec.replicas}}{{\$rs.metadata.name}}{{end}}{{end}}{{end}}{{end}}{{end}}")
  messages=$(k8s get pod -n "${namespace}" -o go-template="{{range .items}}{{\$pod := .}}{{range .metadata.ownerReferences}}{{if eq .name \"${replica}\"}}{{\$pod.metadata.name}} : {{range \$pod.status.conditions}}{{.message}}~{{end}}{{end}}{{end}}{{end}}" | tr '~' '\n')

  echo "${messages}"

  echo "$(get_logs resource=replicaset $(get_deployment_new_replicaset "${deployment}"))"
}

check_for_timeout() {
  if is_timeout_exceeded
  then
    echo timed out after $(get_time_spent) seconds
    report_deployment_issues
    exit 3
  fi
}

wait_to_check() {
  check_for_timeout
  sleep ${check_interval}
}

readonly start_time=$(get_timestamp)

readonly generation=$(get_generation)
echo "waiting for specified generation ${generation} to be observed"
while [[ $(get_observed_generation) -lt ${generation} ]]; do
  wait_to_check
done
echo "specified generation observed."

available=-1
desired=-1
total=-1
unavailable=-1
updated=-1
ready=-1
while [[ ${total} -ne ${desired} || ${updated} -ne ${desired} || ${unavailable} -ne 0 || ${ready} -ne ${desired} ]]; do
  wait_to_check
  read desired updated total available unavailable <<<$(k8s describe deployment ${deployment} --namespace=${namespace} | grep 'Replicas:' | awk '{ print $2,$5,$8,$11,$14 }')
  ready=$(get_ready_replicas)
  echo "desired: ${desired}, total: ${total}, available: ${available}, updated: ${updated}, unavailable: ${unavailable}, ready: ${ready}"
  get_pod_images
done

echo "deployment complete."
