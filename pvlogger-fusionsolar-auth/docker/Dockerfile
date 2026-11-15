FROM golang:1.27.4 AS golang

COPY . /
WORKDIR /pvlogger-fusionsolar-auth

# Module and build caches survive between builds (BuildKit; backlog 58).
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger FusionSolar Auth Service"
LABEL version="3.4.3"

COPY --from=golang /fusionsolar-auth .

ENTRYPOINT ["/fusionsolar-auth"]
