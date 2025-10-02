#!/usr/bin/env bash
#
# Script: replace_path_by_home.sh
# Purpose: Replace REPLACE_PATH placeholder with user's HOME directory in files
# Dependencies: sed, cat
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

SED_CMD=$(${REQUIRE_COMMAND} sed)
CAT_CMD=$(${REQUIRE_COMMAND} cat)

"$CAT_CMD" "$@" | "$SED_CMD" "s|REPLACE_PATH|${HOME}|"
