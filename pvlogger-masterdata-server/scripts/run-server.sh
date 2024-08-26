#!/usr/bin/env bash

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

OP_CMD=""

if hash op 2>/dev/null; then
  OP_CMD=$(command -v op)
else
  echo "op command not found"
  exit 1
fi

DB_PASSWORD=$(${OP_CMD} read "op://Personal/PV Logger Postgres DB/password")

# determine script location
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

if [ $# -eq 1 ] && [ "${1}" == "prod" ]; then
  echo "Running in prod mode";
  MASTER_DATA_PATH="/opt/pvlogger/masterdata" DB_PASSWORD="${DB_PASSWORD}" "${SCRIPT_DIR}"/../gradlew run --args="-config=application-prod.conf";
elif [ $# -eq 1 ] && [ "${1}" == "staging" ]; then
  echo "Running in staging mode";
  MASTER_DATA_PATH="${HOME}/Documents/Solaranalage/Huawei/Masterdata/Modbus" DB_PASSWORD="${DB_PASSWORD}" "${SCRIPT_DIR}"/../gradlew run --args="-config=application-staging.conf";
else
  echo "Running in dev mode";
  "${SCRIPT_DIR}"/../gradlew run --args="-config=application-dev.conf -P:database.password='${DB_PASSWORD}'";
fi
