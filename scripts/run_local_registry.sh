#!/usr/bin/env bash
#
# Script: run_local_registry.sh
# Purpose: Start a local Docker registry container for development
# Dependencies: docker
# Exit codes: 0=success, 1=error, 127=missing dependency (from require-command.sh)
#

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

# determine script location
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
REQUIRE_COMMAND="${SCRIPT_DIR}/require-command.sh"

DOCKER_CMD=$(${REQUIRE_COMMAND} docker)
REGISTRY_VERSION="2.8.3"

$DOCKER_CMD run --rm -d -p 15000:5000 --name registry -v "${HOME}"/registry:/var/lib/registry registry:"${REGISTRY_VERSION}"
