#!/usr/bin/env bash

if ! kubectl get service traefik --namespace traefik 2>/dev/null >/dev/null; then
    helm install \
        traefik \
        traefik/traefik \
        --namespace=traefik --create-namespace >/dev/null
fi