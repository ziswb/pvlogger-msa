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
REGISTRY_PATH="bziswiler"

help()
{
   echo "This script builds a docker image for pvlogger."
   echo
   echo "Syntax: build.sh [-v|h] <pvlogger-component>"
   echo "options:"
   echo "h     Print this Help."
   echo "v     Version to tag the docker image with."
   echo
}

version=""

while getopts v:h flag
do
  case "${flag}" in
    v) version=${OPTARG}
       shift "$((OPTIND-1))"
       ;;
    h) help
       exit 0
       ;;
    *) echo "Invalid option: -$OPTARG"
       exit 0
       ;;
  esac
done

PV_LOGGER_COMPONENT="$*"

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

echo "Building pv logger component '${PV_LOGGER_COMPONENT}' version '${version}' and pushing to '${DOCKER_REGISTRY}/${REGISTRY_PATH}'"

$DOCKER_CMD build \
-t "${DOCKER_REGISTRY}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:latest" \
-t "${DOCKER_REGISTRY}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:${version}" \
-f "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}/docker/Dockerfile" \
"${SCRIPT_DIR}/../"
$DOCKER_CMD image push "${DOCKER_REGISTRY}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:latest"
$DOCKER_CMD image push "${DOCKER_REGISTRY}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:${version}"
