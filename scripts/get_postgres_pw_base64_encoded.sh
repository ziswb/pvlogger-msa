#!/usr/bin/env bash
#
# Script: get_postgres_pw_base64_encoded.sh
# Purpose: Retrieve PostgreSQL password from 1Password and encode as base64
# Dependencies: op (1Password CLI)
# Exit codes: 0=success, 1=error, 127=missing dependency (from require-command.sh)
#

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

# Configuration - can be overridden via environment variables
ONEPASSWORD_ACCOUNT="${ONEPASSWORD_ACCOUNT:-my.1password.com}"
POSTGRES_VAULT="${POSTGRES_VAULT:-Private}"
POSTGRES_ITEM="${POSTGRES_ITEM:-PV Logger Postgres DB}"

OP_CMD=$(${REQUIRE_COMMAND} op)

echo "Retrieving PostgreSQL password from 1Password..." >&2
POSTGRES_PASSWORD=$(${OP_CMD} read --account "${ONEPASSWORD_ACCOUNT}" "op://${POSTGRES_VAULT}/${POSTGRES_ITEM}/password")
echo -n "${POSTGRES_PASSWORD}" | base64
