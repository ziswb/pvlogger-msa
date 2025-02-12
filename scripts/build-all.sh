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

declare -a pids

waitPids() {
    while [ ${#pids[@]} -ne 0 ]; do
        echo "Waiting for pids:" "${pids[@]}"
        local range
        range=$(eval "echo {0..$((${#pids[@]}-1))}")
        local i
        for i in $range; do
            if ! kill -0 "${pids[$i]}" 2> /dev/null; then
                echo "Done -- ${pids[$i]}"
                unset 'pids[$i]'
            fi
        done
        pids=("${pids[@]}") # Expunge nulls created by unset.
        sleep 1
    done
    echo "Done!"
}

addPid() {
    local desc=$1
    local pid=$2
    echo "$desc -- $pid"
    pids=("${pids[@]}" "$pid")
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
  "pvlogger-fusionsolar"
  "pvlogger-heartbeat"
  "pvlogger-kpi-aggregator"
  "pvlogger-kpi-exporter"
  "pvlogger-masterdata-server"
  "pvlogger-modbus-data-accessor"
  "pvlogger-modbus-data-collector"
  "pvlogger-modbus-mock-server"
  "pvlogger-sunrise-sunset"
)

echo "###############################################################"
echo "                       START BUILDING"
echo "###############################################################"

for module in "${PV_LOGGER_MODULES[@]}"; do
  echo "executing ${SCRIPT_DIR}/build.sh ${passed_ops} -c $module"
  eval "${SCRIPT_DIR}/build.sh ${passed_ops} -c $module" >/dev/null 2>&1 &
  addPid "Building $module" $!
done

waitPids

echo "###############################################################"
echo "                        BUILDING DONE"
echo "###############################################################"

#  "${SCRIPT_DIR}/build.sh" -v "$(date +%s)" -l -c $module
