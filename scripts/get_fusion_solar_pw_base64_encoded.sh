#!/usr/bin/env bash
#
# Script: get_fusion_solar_pw_base64_encoded.sh
# Purpose: Retrieve Huawei FusionSolar password from 1Password and encode as base64
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
FUSION_SOLAR_VAULT="${FUSION_SOLAR_VAULT:-Private}"
FUSION_SOLAR_ITEM="${FUSION_SOLAR_ITEM:-Huawei FusionSolar openAPI Account}"

OP_CMD=$(${REQUIRE_COMMAND} op)

echo "Retrieving FusionSolar password from 1Password..." >&2
FUSION_SOLAR_PASSWORD=$(${OP_CMD} read --account "${ONEPASSWORD_ACCOUNT}" "op://${FUSION_SOLAR_VAULT}/${FUSION_SOLAR_ITEM}/password")
echo -n "${FUSION_SOLAR_PASSWORD}" | base64
