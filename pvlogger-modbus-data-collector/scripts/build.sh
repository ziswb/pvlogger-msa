#!/usr/bin/env bash

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

DOCKER_CMD=""
GREP_CMD=""
SED_CMD=""

help()
{
   echo "This script builds the docker image for pvlogger data collector."
   echo
   echo "Syntax: build.sh [-v|h]"
   echo "options:"
   echo "h     Print this Help."
   echo "v     Version to tag the docker image with."
   echo
}

if hash grep 2>/dev/null; then
  GREP_CMD=$(command -v grep)
else
  echo "grep command not found"
  exit 1
fi

if hash sed 2>/dev/null; then
  SED_CMD=$(command -v sed)
else
  echo "sed command not found"
  exit 1
fi

if hash docker 2>/dev/null; then
  DOCKER_CMD=$(command -v docker)
else
  echo "docker command not found"
  exit 1
fi

# determine script location
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

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
  version="$(${GREP_CMD} -e 'LABEL version=\"\([0-9.]*\)\"' "${SCRIPT_DIR}"/../docker/Dockerfile | ${SED_CMD} -e 's/LABEL version=\"\([0-9.]*\)\"/\1/')";
fi

if [ -z "${version// }" ]; then
  echo "Version not set"
  exit
fi

if [ -z "$(curl -s --unix-socket /var/run/docker.sock http/_ping 2>&1 /dev/null)" ]; then
  echo "Docker is not running";
  exit;
fi

"${SCRIPT_DIR}"/jdk.sh './gradlew clean shadowJar'

cp "${SCRIPT_DIR}"/../build/libs/*-all.jar "${SCRIPT_DIR}"/../docker/

$DOCKER_CMD buildx build --load --platform=linux/arm64/v8 \
-t registry.local:5000/bziswiler/pvlogger-modbus-data-collector:latest \
-t registry.local:5000/bziswiler/pvlogger-modbus-data-collector:"${version}" \
"${SCRIPT_DIR}"/../docker/
$DOCKER_CMD image push registry.local:5000/bziswiler/pvlogger-modbus-data-collector:latest
$DOCKER_CMD image push registry.local:5000/bziswiler/pvlogger-modbus-data-collector:"${version}"

rm "${SCRIPT_DIR}"/../docker/*-all.jar
