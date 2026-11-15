FROM golang:1.27.4 AS golang

COPY . /
WORKDIR /pvlogger-fusionsolar

# Module and build caches survive between builds (BuildKit; backlog 58).
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Fusion Solar"
LABEL version="3.5.5"

COPY --from=golang /fusionsolar .

ENTRYPOINT ["/fusionsolar"]