#!/usr/bin/env bash

set -o errexit
set -o pipefail

k8s.init() {
  mkdir ~/.kube
  echo "${KUBE_CONFIG}" > ~/.kube/config
  kubectl config use-context "${KUBE_CONTEXT:-default}"
}

k8s.init > /dev/null
