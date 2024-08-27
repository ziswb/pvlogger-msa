#!/usr/bin/env bash

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

OP_CMD=""
PG_HOST=""
PG_PORT=""
MQ_HOST=""
MQ_PORT=""

if hash op 2>/dev/null; then
  OP_CMD=$(command -v op)
else
  echo "op command not found"
  exit 1
fi

DB_PWD=$(${OP_CMD} read "op://Personal/PV Logger Postgres DB/password")

# determine script location
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

DB_PASSWORD="${DB_PWD}" POSTGRESQL_HOST="${PG_HOST}" POSTGRESQL_PORT="${PG_PORT}" ACTIVEMQ_HOST="${MQ_HOST}" ACTIVEMQ_PORT="${MQ_PORT}" "${SCRIPT_DIR}"/../gradlew run --args="-config=application-prod.conf"

# Pass arguments alternatively:
# ./gradlew run --args="-P:database.password='${DB_PASSWORD}'"
