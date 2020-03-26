#!/usr/bin/env bash

set -o errexit
set -o pipefail

describe() {
  local baseRevision=${1:-HEAD}
  local revision=${2:-HEAD}
  echo -n "{"
  echo -n $(git log -1 --pretty=format:'"hash":"%H","timestamp":%ct' ${baseRevision}),
  echo -n "\"tag\":\"$(git describe --tags --always ${baseRevision})\","
  echo -n "\"revision\":$(git rev-list --count ${baseRevision}),"
  echo -n "\"diff\":{"
  echo -n "\"files\":["
  IFS=$'\n'
  local files=($(git diff --name-only ${baseRevision} ${revision}))
  echo -n "\"${files[0]}\""
  for ((i=1;i<${#files[@]};i++))
  do
    echo -n ",\"${files[${i}]}\""
  done
  echo -n "]"
  echo -n "}"
  echo -n "}"
}

echo $(describe ${1:-HEAD} ${2:-${COMMIT}})
