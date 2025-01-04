#!/usr/bin/env bash

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
