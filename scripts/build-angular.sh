#!/usr/bin/env bash
#
# Script: build-angular.sh
# Purpose: Build Angular frontend using Docker container
# Dependencies: docker
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
NODE_IMAGE="node:25.7.0-alpine3.23"

$DOCKER_CMD run \
--interactive \
--rm \
--name "pvlogger-frontend-angular-build" \
--volume "${SCRIPT_DIR}/../pvlogger-frontend/src/":/usr/src/app \
--workdir /usr/src/app/web \
--env CHROME_BIN=/usr/bin/chromium-browser \
"$NODE_IMAGE" \
sh -c "apk add --no-cache chromium && npm install && npx ng test --watch=false --browsers=ChromeHeadlessNoSandbox && npx ng build --configuration=production"
