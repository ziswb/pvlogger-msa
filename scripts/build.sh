#!/usr/bin/env bash
#
# Script: build.sh
# Purpose: Build Docker images for PV Logger components
# Dependencies: docker, grep, sed
# Exit codes: 0=success, 1=error, 3=version tag already in the registry (nothing built),
#             127=missing dependency (from require-command.sh)
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
CURL_CMD=$(${REQUIRE_COMMAND} curl)
# Site settings (the private registry host) live outside this public repo, in
# $PVLOGGER_LOCAL_ENV (default ~/.config/pvlogger/local.env), or in the environment.
PVLOGGER_LOCAL_ENV="${PVLOGGER_LOCAL_ENV:-${HOME}/.config/pvlogger/local.env}"
if [ -f "${PVLOGGER_LOCAL_ENV}" ]; then
  # shellcheck source=/dev/null
  . "${PVLOGGER_LOCAL_ENV}"
fi
DOCKER_REGISTRY="${DOCKER_REGISTRY:-}"
LOCAL_REGISTRY="localhost:15000"
REGISTRY_PATH="bziswiler"

help()
{
   echo "This script builds a docker image for pvlogger."
   echo
   echo "Syntax: build.sh [-h|-l|-f|-v <version>] -c <pvlogger-component>"
   echo "options:"
   echo "h     Print this Help."
   echo "v     The <version> to tag the docker image with."
   echo "l     Use the local docker registry."
   echo "f     Force: push even if the version tag already exists in the registry."
   echo "      Without it, an existing tag stops the build with exit code 3, so a"
   echo "      released image is never silently replaced."
   echo
}

version=""
component=""
force=0
registry="$DOCKER_REGISTRY"

OPTSTRING=":v:c:lfh"

while getopts $OPTSTRING option
do
  case "${option}" in
    v) version=${OPTARG}
       echo "using version '$version'"
       ;;
    l) registry="${LOCAL_REGISTRY}"
       echo "using local registry '${registry}'"
       ;;
    f) force=1
       echo "forcing: an existing version tag will be overwritten"
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

if [ -z "${registry}" ]; then
  echo "Error: no registry. Set DOCKER_REGISTRY (host:port) in ${PVLOGGER_LOCAL_ENV}" >&2
  echo "or the environment, or use -l for the local registry." >&2
  exit 1
fi

# Validate component directory exists
if [[ ! -d "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}" ]]; then
    echo "Error: Component directory not found: ${PV_LOGGER_COMPONENT}" >&2
    echo "Available components in parent directory:" >&2
    found_component=0
    for dir in "${SCRIPT_DIR}/../"*/; do
        if [ -f "${dir}docker/Dockerfile" ]; then
            echo "$(basename "$dir")"
            found_component=1
        fi
    done
    if [ "$found_component" -eq 0 ]; then
        echo "  No buildable components found" >&2
    fi
    exit 1
fi

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

# Validate Dockerfile exists
if [[ ! -f "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}/docker/Dockerfile" ]]; then
    echo "Error: Dockerfile not found at: ${PV_LOGGER_COMPONENT}/docker/Dockerfile" >&2
    exit 1
fi

# Released tags are immutable: refuse to overwrite one unless forced. Checked
# before building, so an unchanged module costs nothing. A registry that cannot
# be asked is an error, not "tag absent".
TAG_EXISTS_EXIT=3
registry_scheme="https"
if [ "${registry}" = "${LOCAL_REGISTRY}" ]; then
  registry_scheme="http"
fi
manifest_url="${registry_scheme}://${registry}/v2/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}/manifests/${version}"
manifest_status="$($CURL_CMD -s -o /dev/null -m 15 -I -w '%{http_code}' \
  -H 'Accept: application/vnd.oci.image.index.v1+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.docker.distribution.manifest.v2+json' \
  "${manifest_url}" || true)"
case "${manifest_status}" in
  200)
    if [ "${force}" -eq 1 ]; then
      echo "WARNING: ${PV_LOGGER_COMPONENT}:${version} already exists in ${registry}; overwriting it (-f)."
    else
      echo "${PV_LOGGER_COMPONENT}:${version} already exists in ${registry}; not rebuilding." >&2
      echo "Bump LABEL version in ${PV_LOGGER_COMPONENT}/docker/Dockerfile, or pass -f to overwrite on purpose." >&2
      exit "${TAG_EXISTS_EXIT}"
    fi
    ;;
  404)
    ;;
  *)
    echo "Error: cannot check ${manifest_url} (HTTP status '${manifest_status}')." >&2
    exit 1
    ;;
esac

echo "Building pv logger component '${PV_LOGGER_COMPONENT}' version '${version}' and pushing to '${registry}/${REGISTRY_PATH}'"

# Lock file for serializing docker push operations across parallel builds
PUSH_LOCK_FILE="${SCRIPT_DIR}/../build/.docker-push.lock"

# Only the version tag: no :latest. Every deployment pins a version, and a
# moving :latest only leaves untagged manifests behind in the registry.
# The cluster runs arm64. The Go images cross-compile for the target platform
# since backlog 56, so the platform is pinned here instead of following the
# build host. PVLOGGER_PLATFORM overrides it (e.g. linux/amd64).
$DOCKER_CMD build \
--network host \
--platform "${PVLOGGER_PLATFORM:-linux/arm64}" \
-t "${registry}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:${version}" \
-f "${SCRIPT_DIR}/../${PV_LOGGER_COMPONENT}/docker/Dockerfile" \
"${SCRIPT_DIR}/../"

# Use flock to serialize pushes across parallel build processes (timeout 10 min)
flock --timeout 600 "$PUSH_LOCK_FILE" $DOCKER_CMD image push "${registry}/${REGISTRY_PATH}/${PV_LOGGER_COMPONENT}:${version}"
