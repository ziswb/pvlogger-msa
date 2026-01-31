#!/usr/bin/env bash
#
# Script: build.sh
# Purpose: Build Docker images for PV Logger components
# Dependencies: docker, grep, sed
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

DOCKER_CMD=$(${REQUIRE_COMMAND} docker)
GREP_CMD=$(${REQUIRE_COMMAND} grep)
SED_CMD=$(${REQUIRE_COMMAND} sed)
DOCKER_REGISTRY="registry.local:5000"
LOCAL_REGISTRY="localhost:15000"
REGISTRY_PATH="bziswiler"

help()
{
   echo "This script builds a docker image for pvlogger."
   echo
   echo "Syntax: build.sh [-h|-l|-v <version>] -c <pvlogger-component>"
   echo "options:"
   echo "h     Print this Help."
   echo "v     The <version> to tag the docker image with."
   echo "l     Use the local docker registry."
   echo
}

version=""
component=""
registry="$DOCKER_REGISTRY"

OPTSTRING=":v:c:lh"

while getopts $OPTSTRING option
do
  case "${option}" in
    v) version=${OPTARG}
       echo "using version '$version'"
       ;;
    l) registry="${LOCAL_REGISTRY}"
       echo "using local registry '${registry}'"
       ;;
    c) component="${OPTARG}"
       echo "building component '${component}'"
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

if [ -z "${component}" ]; then
  echo "-----------------------------------"
  echo "Option '-c <component>' is missing."
  echo "-----------------------------------"
  help
  exit 1
fi

PV_LOGGER_COMPONENT="$component"

if [ -z "${version}" ]; then
  version="$(${GREP_CMD} -e 'LABEL version="\([0-9.]*\)"' "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}/docker/Dockerfile" | ${SED_CMD} -e 's/LABEL version=\"\([0-9.]*\)\"/\1/')";
fi

if [ -z "${version// }" ]; then
  echo "Version not set"
  exit 1
fi

# Validate Docker daemon is accessible
if ! docker info >/dev/null 2>&1; then
    echo "Error: Docker daemon not accessible. Please ensure Docker is running." >&2
    exit 1
fi

# Validate component directory exists
if [[ ! -d "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}" ]]; then
    echo "Error: Component directory not found: ${PV_LOGGER_COMPONENT}" >&2
    echo "Available components in parent directory:" >&2
    ls -1 "${SCRIPT_DIR}/../" | grep "^pvlogger-" || echo "  No pvlogger components found" >&2
    exit 1
fi

# Validate Dockerfile exists
if [[ ! -f "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}/docker/Dockerfile" ]]; then
    echo "Error: Dockerfile not found at: ${PV_LOGGER_COMPONENT}/docker/Dockerfile" >&2
    exit 1
fi

echo "Building pv logger component '${PV_LOGGER_COMPONENT}' version '${version}' and pushing to '${registry}/${REGISTRY_PATH}'"

$DOCKER_CMD build \
--network host \
-t "${registry}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:latest" \
-t "${registry}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:${version}" \
-f "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}/docker/Dockerfile" \
"${SCRIPT_DIR}/../"
$DOCKER_CMD image push "${registry}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:latest"
$DOCKER_CMD image push "${registry}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:${version}"
