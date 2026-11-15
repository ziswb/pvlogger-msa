FROM golang:1.27.4 AS golang

COPY . /
WORKDIR /pv-fast-data-collector

# Module and build caches survive between builds (BuildKit; backlog 58).
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PV Fast Data Collector - focused KPI register reader and KPI computer"
LABEL version="0.13.5"

COPY --from=golang /fast-data-collector .

ENTRYPOINT ["/fast-data-collector"]