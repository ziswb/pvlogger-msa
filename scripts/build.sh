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
   echo "Syntax: build.sh [-v|c|l|h] <pvlogger-component>"
   echo "options:"
   echo "h     Print this Help."
   echo "v     Version to tag the docker image with."
   echo "l     Use local docker registry."
   echo "c     The component to build."
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

PV_LOGGER_COMPONENT="$component"

if [ -z "${version}" ]; then
  version="$(${GREP_CMD} -e 'LABEL version=\"\([0-9.]*\)\"' "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}/docker/Dockerfile" | ${SED_CMD} -e 's/LABEL version=\"\([0-9.]*\)\"/\1/')";
fi

if [ -z "${version// }" ]; then
  echo "Version not set"
  exit 1
fi

if [ -z "$(curl -s --unix-socket /var/run/docker.sock http/_ping 2>&1 /dev/null)" ]; then
  echo "Docker is not running";
  exit 1;
fi

echo "Building pv logger component '${PV_LOGGER_COMPONENT}' version '${version}' and pushing to '${registry}/${REGISTRY_PATH}'"

$DOCKER_CMD build \
-t "${registry}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:latest" \
-t "${registry}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:${version}" \
-f "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}/docker/Dockerfile" \
"${SCRIPT_DIR}/../"
$DOCKER_CMD image push "${registry}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:latest"
$DOCKER_CMD image push "${registry}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:${version}"
