# Multi-arch: the cluster runs arm64, the relay on the receiver's server
# usually amd64. The build stage runs natively and cross-compiles.
FROM --platform=$BUILDPLATFORM golang:1.27.4 AS golang
ARG TARGETARCH=arm64

COPY . /
WORKDIR /pvlogger-kpi-exporter

# Module and build caches survive between builds (BuildKit; backlog 58).
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    make prod GOARCH=${TARGETARCH}

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger KPI Exporter"
LABEL version="3.5.2"

COPY --from=golang /exporter .

ENTRYPOINT ["/exporter"]
