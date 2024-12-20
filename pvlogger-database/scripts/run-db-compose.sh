#!/usr/bin/env bash

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

OP_CMD=""
DOCKER_COMPOSE_CMD=""

if hash op 2>/dev/null; then
  OP_CMD=$(command -v op)
else
  echo "op command not found"
  exit 1
fi

if hash docker-compose 2>/dev/null; then
  DOCKER_COMPOSE_CMD=$(command -v docker-compose)
else
  echo "docker-compose command not found"
  exit 1
fi

# determine script location
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
DB_DATA_PATH="${SCRIPT_DIR}/db-data"
DB_PASSWORD=$(${OP_CMD} read "op://Personal/PV Logger Postgres DB/password")

DB_PASSWORD="${DB_PASSWORD}" \
DB_DATA_PATH="${DB_DATA_PATH}" \
${DOCKER_COMPOSE_CMD} -f "${SCRIPT_DIR}"/../docker/docker-compose.yaml up -d
