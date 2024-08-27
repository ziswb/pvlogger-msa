#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

command="${1}"
RESOLVED_COMMAND=""


if hash "${command}" 2>/dev/null; then
  RESOLVED_COMMAND=$(command -v "${command}")
else
  (echo >&2 "ERROR: ${1} is not installed.")
  (echo >&2 "Make sure ${1} is installed and on your PATH.")
  exit 1
fi

echo "${RESOLVED_COMMAND}"
