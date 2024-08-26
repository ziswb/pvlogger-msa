#!/usr/bin/env bash

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

DOCKER_COMPOSE_CMD=""

if hash docker-compose 2>/dev/null; then
  DOCKER_COMPOSE_CMD=$(command -v docker-compose)
else
  echo "docker-compose command not found"
  exit 1
fi

# determine script location
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

DB_PASSWORD="" \
DB_DATA_PATH="" \
DB_INIT_SCRIPT_PATH="" \
${DOCKER_COMPOSE_CMD} -f "${SCRIPT_DIR}"/../docker/docker-compose.yaml down
