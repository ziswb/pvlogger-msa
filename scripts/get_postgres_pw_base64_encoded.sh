#!/usr/bin/env bash

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

#------------------------------------------------------------------------------
SCRIPT_DIR=$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
REQUIRE_COMMAND="${SCRIPT_DIR}/require-command.sh"
#------------------------------------------------------------------------------

OP_CMD=$(${REQUIRE_COMMAND} op)

POSTGRES_PASSWORD=$(${OP_CMD} read --account my.1password.com "op://Private/PV Logger Postgres DB/password")
echo -n "${POSTGRES_PASSWORD}" | base64
