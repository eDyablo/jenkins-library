#!/usr/bin/env bash

k8s() {
  kubectl "${KUBECONFIG:+--kubeconfig=${KUBECONFIG}}" "${CONTEXT:+--context=${CONTEXT}}" "${@}"
}

resources=$(k8s get $KIND -n $NAMESPACE -o json | jq --arg pattern "$NAMEPATTERN" '.items | sort_by(.metadata.creationTimestamp) | .[] | select(.metadata.name | contains($pattern)).metadata.name')
resources2delete=$(echo "$resources" | grep -Po "$NAMEPATTERN-\d.*" | head --lines=-$KEEPNUMBER | tr -d '"')

if [ -n "$resources2delete" ]; then
  k8s delete $KIND $(echo "$resources2delete") -n $NAMESPACE
  echo "$resources2delete"
fi
