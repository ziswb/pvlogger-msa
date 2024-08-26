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

./gradlew run --args="-P:database.password='${DB_PASSWORD}'"
