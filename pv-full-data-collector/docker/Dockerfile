FROM golang:1.27.4 AS golang

COPY . /
WORKDIR /pv-full-data-collector

# Module and build caches survive between builds (BuildKit; backlog 58).
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PV Full Data Collector - reads all dynamic Modbus register values on demand"
LABEL version="0.8.6"

COPY --from=golang /full-data-collector .

ENTRYPOINT ["/full-data-collector"]
