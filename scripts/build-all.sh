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
   echo "Syntax: build-all.sh [-v VERSION] [-l] [-f] [-h]"
   echo "options:"
   echo "h     Print this Help."
   echo "v     Version to tag the docker image with."
   echo "l     Use local docker registry."
   echo "f     Force: overwrite version tags that already exist in the registry."
   echo
}

declare -a pids=()           # Indexed array: list of background process PIDs
declare -A pid_to_module     # Associative array: maps PID → module name for error reporting
declare -a failed_modules=() # Indexed array: collects module names that failed
declare -a skipped_modules=() # Indexed array: version already in the registry (build.sh exit 3)

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
                local pid="${pids[$i]}"
                local module="${pid_to_module[$pid]}"
                # Use wait to get exit code
                if wait "$pid"; then
                    echo "Done (success) -- $module ($pid)"
                else
                    local exit_code=$?
                    if [ "$exit_code" -eq 3 ]; then
                        echo "Skipped (version already in registry) -- $module ($pid)"
                        skipped_modules+=("$module")
                    else
                        echo "Done (FAILED with exit code $exit_code) -- $module ($pid)" >&2
                        failed_modules+=("$module")
                    fi
                fi
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
                failed_modules+=("${pid_to_module[$pid]}")
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
    
    if [ ${#skipped_modules[@]} -ne 0 ]; then
        echo ""
        echo "SKIPPED (version already in the registry; bump LABEL version or pass -f):"
        for module in "${skipped_modules[@]}"; do
            echo "  - $module"
        done
    fi

    # Report failures
    if [ ${#failed_modules[@]} -ne 0 ]; then
        echo "" >&2
        echo "FAILED BUILDS:" >&2
        for module in "${failed_modules[@]}"; do
            echo "  - $module (see build/${module}-build.log)" >&2
        done
        return 1
    fi
    
    echo "All builds completed successfully!"
}

addPid() {
    local module=$1
    local pid=$2
    echo "Building $module -- $pid"
    pids=("${pids[@]}" "$pid")
    pid_to_module[$pid]="$module"
}

OPTSTRING=":v:lfh"
declare -a passed_args=()

while getopts $OPTSTRING option
do
  case "${option}" in
    v) passed_args+=("-v" "${OPTARG}")
       ;;
    l) passed_args+=("-l")
       ;;
    f) passed_args+=("-f")
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
  "pv-fast-data-collector"
  "pv-full-data-collector"
  "pvlogger-frontend"
  "pvlogger-fusionsolar"
  "pvlogger-fusionsolar-auth"
  "pvlogger-kpi-exporter"
  "pvlogger-masterdata-server"
  "pvlogger-modbus-data-accessor"
  "pv-device-data-collector"
  "pvlogger-modbus-mock"
)

echo "cleaning build logs"
if [ -d "${SCRIPT_DIR}"/../build ]; then
 rm -rf "${SCRIPT_DIR}"/../build/pvlogger-*-build.log
else
  mkdir "${SCRIPT_DIR}"/../build
fi

echo "###############################################################"
echo "                       START BUILDING"
echo "###############################################################"

for module in "${PV_LOGGER_MODULES[@]}"; do
  echo "executing ${SCRIPT_DIR}/build.sh ${passed_args[*]} -c ${module}"
  "${SCRIPT_DIR}/build.sh" "${passed_args[@]}" -c "${module}" >"${SCRIPT_DIR}/../build/${module}-build.log" 2>&1 &
  addPid "${module}" $!
done

if ! waitPids; then
    echo "###############################################################" >&2
    echo "                     BUILDING FAILED" >&2
    echo "###############################################################" >&2
    exit 1
fi

echo "###############################################################"
echo "                        BUILDING DONE"
echo "###############################################################"
