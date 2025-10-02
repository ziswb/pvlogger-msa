#!/usr/bin/env bash
#
# Script: install_traefik.sh
# Purpose: Install Traefik ingress controller via Helm if not already present
# Dependencies: kubectl, helm
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

# Validate required commands
KUBECTL_CMD=$(${REQUIRE_COMMAND} kubectl)
HELM_CMD=$(${REQUIRE_COMMAND} helm)

echo "Checking if Traefik is already installed..."

# Check if already installed
if ${KUBECTL_CMD} get service traefik --namespace traefik >/dev/null 2>&1; then
    echo "Traefik is already installed in the traefik namespace"
    exit 0
fi

echo "Installing Traefik..."
${HELM_CMD} repo add traefik https://traefik.github.io/charts
${HELM_CMD} repo update
${HELM_CMD} install \
    traefik \
    traefik/traefik \
    --namespace=traefik --create-namespace

echo "Traefik installation completed successfully"