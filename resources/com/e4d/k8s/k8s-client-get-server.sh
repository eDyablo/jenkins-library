#!/usr/bin/env bash

set -o errexit

trap "exit 1" TERM

k8s() {
  kubectl "${KUBECONFIG:+--kubeconfig=${KUBECONFIG}}" "${CONTEXT:+--context=${CONTEXT}}" "${@}"
}

get_config_go() {
  local readonly _go="$1"
  k8s config view --output "go-template=${_go}"
  if [ $? != 0 ]; then
    kill -s TERM $$
  fi
}

get_current_context() {
  k8s config current-context
  if [ $? != 0 ]; then
    kill -s TERM $$
  fi
}

get_cluster() {
  local readonly _context="$1"
  get_config_go "{{ range .contexts -}} {{ if eq .name \"$_context\" -}} {{ .context.cluster }} {{- end }} {{- end }}"
}

get_server() {
  local readonly _cluster="$1"
  get_config_go "{{ range .clusters -}} {{ if eq .name \"$_cluster\" -}} {{ .cluster.server }} {{- end }} {{- end }}"
}

get_creds() {
  local readonly _cluster="$1"
  get_config_go "{{ range .users -}} {{ if eq .name \"$_cluster\" -}} {{ .user.username }}:{{ .user.password }} {{- end }} {{- end }}"
}

readonly context="${1:-${CONTEXT:-$(get_current_context)}}"
readonly cluster="$(get_cluster $context)"
readonly server="$(get_server $cluster)"
readonly creds="$(get_creds $cluster)"

echo "$server" $(echo -n "$creds" | base64)
