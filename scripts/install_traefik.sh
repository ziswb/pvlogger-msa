#!/usr/bin/env bash

if ! kubectl get service traefik --namespace traefik 2>/dev/null >/dev/null; then
    helm repo add traefik https://traefik.github.io/charts
    helm repo update
    helm install \
        traefik \
        traefik/traefik \
        --namespace=traefik --create-namespace >/dev/null
fi