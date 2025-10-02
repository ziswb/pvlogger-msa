#!/usr/bin/env bash
#
# Script: require-command.sh
# Purpose: Validate that a required command is available and return its path
# Dependencies: none
# Exit codes: 0=success, 2=usage error (missing argument), 127=missing dependency (command not found)
#

set -o errexit
set -o pipefail
set -o nounset

# Validate argument provided
if [[ $# -eq 0 ]]; then
    echo "Usage: $0 <command>" >&2
    echo "Error: No command specified" >&2
    exit 2
fi

command="${1}"
RESOLVED_COMMAND=""


if hash "${command}" 2>/dev/null; then
  RESOLVED_COMMAND=$(command -v "${command}")
else
  (echo >&2 "ERROR: ${1} is not installed.")
  (echo >&2 "Make sure ${1} is installed and on your PATH.")
  exit 127
fi

echo "${RESOLVED_COMMAND}"
