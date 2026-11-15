FROM golang:1.27.4 AS golang

COPY . /
WORKDIR /pvlogger-modbus-mock

# Module and build caches survive between builds (BuildKit; backlog 58).
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PVLogger Modbus mock - the synthetic plant over Modbus TCP and its history backfill, for local development"
LABEL version="1.0.1"

COPY --from=golang /mock-server .
# The history backfill runs from the same image: command /backfill.
COPY --from=golang /backfill .

EXPOSE 8502

ENTRYPOINT ["/mock-server"]
