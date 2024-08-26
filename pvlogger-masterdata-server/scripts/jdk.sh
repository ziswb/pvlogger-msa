#!/usr/bin/env bash

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

CMD="${*}"
VERSION="17.0.3_7"

if hash docker 2>/dev/null; then
  DOCKER_CMD=$(command -v docker)
else
  echo "docker command not found"
  exit 1
fi

${DOCKER_CMD} run \
  --rm \
  -v "$(pwd):/workspace" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -u "$(id -u "${USER}"):$(id -g "${USER}")" \
  -e GRADLE_USER_HOME=/workspace/gradle_home \
  "eclipse-temurin:${VERSION}-jdk" \
  sh -ce "cd /workspace && ${CMD}"
