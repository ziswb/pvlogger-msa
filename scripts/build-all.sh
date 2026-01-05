#!/usr/bin/env bash
#
# Script: build-all.sh
# Purpose: Build Docker images for all PV Logger components in parallel
# Dependencies: docker, grep, sed (via build.sh)
# Exit codes: 0=success, 1=error, 124=timeout, 127=missing dependency (from require-command.sh)
#

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

# better diagnostics on failures
trap 'echo "Error on line $LINENO" >&2' ERR

# determine script location (physical path)
SCRIPT_DIR=$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

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
    local timeout=2700  # 45 minutes timeout
    local elapsed=0
    local check_interval=5  # Check every 5 seconds
    
    while [ ${#pids[@]} -ne 0 ] && [ $elapsed -lt $timeout ]; do
        echo "Waiting for pids (${elapsed}s/${timeout}s):" "${pids[@]}"
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
        
        if [ ${#pids[@]} -ne 0 ]; then
            sleep $check_interval
            elapsed=$((elapsed + check_interval))
        fi
    done
    
    # Handle timeout case
    if [ $elapsed -ge $timeout ] && [ ${#pids[@]} -ne 0 ]; then
        echo "Warning: Timeout reached after ${timeout}s, terminating remaining processes..." >&2
        for pid in "${pids[@]}"; do
            if kill -0 "$pid" 2>/dev/null; then
                echo "Terminating process $pid" >&2
                kill -TERM "$pid" 2>/dev/null || true
            fi
        done
        
        # Give processes 10 seconds to terminate gracefully
        sleep 10
        
        # Force kill any remaining processes
        for pid in "${pids[@]}"; do
            if kill -0 "$pid" 2>/dev/null; then
                echo "Force killing process $pid" >&2
                kill -KILL "$pid" 2>/dev/null || true
            fi
        done
        
        echo "Build timeout exceeded. Some builds may have failed." >&2
        exit 124
    fi
    
    echo "All builds completed successfully!"
}

addPid() {
    local desc=$1
    local pid=$2
    echo "$desc -- $pid"
    pids=("${pids[@]}" "$pid")
}

OPTSTRING=":v:lh"
declare -a passed_args=()

while getopts $OPTSTRING option
do
  case "${option}" in
    v) passed_args+=("-v" "${OPTARG}")
       ;;
    l) passed_args+=("-l")
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

echo "passed_args: ${passed_args[*]}"

PV_LOGGER_MODULES=(
  "pvlogger-frontend"
  "pvlogger-fusionsolar"
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
  echo "executing ${SCRIPT_DIR}/build.sh ${passed_args[*]} -c ${module}"
  "${SCRIPT_DIR}/build.sh" "${passed_args[@]}" -c "${module}" >/dev/null 2>&1 &
  addPid "Building ${module}" $!
done

waitPids

echo "###############################################################"
echo "                        BUILDING DONE"
echo "###############################################################"

#  "${SCRIPT_DIR}/build.sh" -v "$(date +%s)" -l -c $module
