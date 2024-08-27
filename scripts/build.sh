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

PV_LOGGER_COMPONENT="$*"

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

while getopts v:h: flag
do
  case "${flag}" in
    v) version=${OPTARG}
       ;;
    h) help
       ;;
    *) echo "Invalid option: -$flag" ;;
  esac
done

if [ -z "${version}" ]; then
  version="$(${GREP_CMD} -e 'LABEL version=\"\([0-9.]*\)\"' "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}/docker/Dockerfile" | ${SED_CMD} -e 's/LABEL version=\"\([0-9.]*\)\"/\1/')";
fi

if [ -z "${version// }" ]; then
  echo "Version not set"
  exit
fi

if [ -z "$(curl -s --unix-socket /var/run/docker.sock http/_ping 2>&1 /dev/null)" ]; then
  echo "Docker is not running";
  exit;
fi

$DOCKER_CMD build \
-t "registry.local:5000/bziswiler/${PV_LOGGER_COMPONENT}:latest" \
-t "registry.local:5000/bziswiler/${PV_LOGGER_COMPONENT}:${version}" \
-f "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}/docker/Dockerfile" \
"${SCRIPT_DIR}/../"
$DOCKER_CMD image push "registry.local:5000/bziswiler/${PV_LOGGER_COMPONENT}:latest"
$DOCKER_CMD image push "registry.local:5000/bziswiler/${PV_LOGGER_COMPONENT}:${version}"
