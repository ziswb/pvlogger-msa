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
NODE_IMAGE="node:23.4-alpine3.21"

$DOCKER_CMD run \
--interactive \
--tty \
--rm \
--name "pvlogger-frontend-angular-build" \
--volume "${SCRIPT_DIR}/../pvlogger-frontend/src/":/usr/src/app \
--workdir /usr/src/app/web \
"$NODE_IMAGE" \
sh -c "npm install && npm run build -- --configuration=production"
