FROM golang:1.27.4 AS golang

ADD . /
WORKDIR /pvlogger-modbus-mock

RUN make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PVLogger Modbus mock - the synthetic plant over Modbus TCP and its history backfill, for local development"
LABEL version="1.0.1"

COPY --from=golang /mock-server .
# The history backfill runs from the same image: command /backfill.
COPY --from=golang /backfill .

EXPOSE 8502

ENTRYPOINT ["/mock-server"]
