#!/usr/bin/env bash

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

# determine script location
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

help()
{
   echo "This script builds a docker image for pvlogger."
   echo
   echo "Syntax: build.sh [-v|l|h] <pvlogger-component>"
   echo "options:"
   echo "h     Print this Help."
   echo "v     Version to tag the docker image with."
   echo "l     Use local docker registry."
   echo
}

OPTSTRING=":v:lh"
passed_ops=""

while getopts $OPTSTRING option
do
  case "${option}" in
    v) passed_ops="${passed_ops} -${option} ${OPTARG}"
       ;;
    l) passed_ops="${passed_ops} -${option}"
       ;;
    h) help
       exit 0
       ;;
    :) echo "Option -${OPTARG} requires an argument."
       exit 1
       ;;
    ?) echo "Invalid option: -${OPTARG}."
       exit 1
       ;;
  esac
done

echo "passed_ops: ${passed_ops}"

PV_LOGGER_MODULES=(
  "pvlogger-frontend"
  "pvlogger-heartbeat"
  "pvlogger-kpi-aggregator"
  "pvlogger-kpi-exporter"
  "pvlogger-masterdata-server"
  "pvlogger-modbus-data-accessor"
  "pvlogger-modbus-data-collector"
  "pvlogger-modbus-mock-server"
)

for module in "${PV_LOGGER_MODULES[@]}"; do
  echo "executing ${SCRIPT_DIR}/build.sh ${passed_ops} -c $module"
  eval "${SCRIPT_DIR}/build.sh ${passed_ops} -c $module"
done

#  "${SCRIPT_DIR}/build.sh" -v "$(date +%s)" -l -c $module
