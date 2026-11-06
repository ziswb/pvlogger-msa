#!/usr/bin/env bash

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

OP_CMD=""
DOCKER_CMD=""
POSTGRES_IMAGE="postgres:16.2"

if hash op 2>/dev/null; then
  OP_CMD=$(command -v op)
else
  echo "op command not found"
  exit 1
fi

if hash docker 2>/dev/null; then
  DOCKER_CMD=$(command -v docker)
else
  echo "docker command not found"
  exit 1
fi

# Personal account by title, any vault (OP_ACCOUNT in the shell is the work one).
DB_PASSWORD=$(${OP_CMD} item get "PV Logger Postgres DB" --account my.1password.com --fields label=password --reveal --format json | jq -r .value)

# determine script location
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
DB_DUMPS_PATH="${SCRIPT_DIR}/../docker/dumps:/dumps"
DB_PGSQL_PATH="${SCRIPT_DIR}/../docker/postgres-data:/var/lib/postgresql/data"

if [ $# -eq 1 ] && [ "${1}" == "prod" ]; then
  echo "Running in prod mode";
  $DOCKER_CMD run -p 5432:5432 --restart unless-stopped -v "${DB_DUMPS_PATH}" -v "${DB_PGSQL_PATH}" --name pvlogger_db -it --shm-size=2gb -e POSTGRES_PASSWORD="${DB_PASSWORD}" $POSTGRES_IMAGE;
else
  echo "Running in dev mode";
  $DOCKER_CMD run -p 5432:5432 --rm                     -v "${DB_DUMPS_PATH}" -v "${DB_PGSQL_PATH}" --name pvlogger_db -it --shm-size=2gb -e POSTGRES_PASSWORD="${DB_PASSWORD}" $POSTGRES_IMAGE;
fi
