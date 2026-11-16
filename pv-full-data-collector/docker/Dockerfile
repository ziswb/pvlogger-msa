# Multi-arch (backlog 56): the build stage runs natively and cross-compiles
# for the target platform.
FROM --platform=$BUILDPLATFORM golang:1.27.4@sha256:5e8afd493a7124a8586e134d9b532731161d4b9becf9dee5a5956b107426f93d AS golang
ARG TARGETARCH

COPY . /
WORKDIR /pv-full-data-collector

# Module and build caches survive between builds (BuildKit; backlog 58).
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    make prod GOARCH=${TARGETARCH}

# nonroot: uid 65532 also outside the chart (plain docker run); the chart
# sets its own uid. Base images pinned by digest (docs/UPGRADING.md).
FROM gcr.io/distroless/static-debian13:nonroot@sha256:aafe993665bf6200db38a02c11a9351e782a84b6a45e7f33b6695638c88e3931

LABEL author="Bruno Ziswiler"
LABEL description="PV Full Data Collector - reads all dynamic Modbus register values on demand"
LABEL version="0.8.9"

COPY --from=golang /full-data-collector /full-data-collector

ENTRYPOINT ["/full-data-collector"]
