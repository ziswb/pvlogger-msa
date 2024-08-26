#!/usr/bin/env bash

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

MD_HOST=""
MD_PORT=""
MQ_HOST=""
MQ_PORT=""

# determine script location
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

MASTERDATA_HOST="${MD_HOST}" MASTERDATA_PORT="${MD_PORT}" ACTIVEMQ_HOST="${MQ_HOST}" ACTIVEMQ_PORT="${MQ_PORT}" "${SCRIPT_DIR}"/../gradlew run --args="-config=application-prod.conf"
