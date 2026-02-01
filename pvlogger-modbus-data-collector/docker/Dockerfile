FROM golang:1.25.4 AS golang

ADD . /
WORKDIR /pvlogger-modbus-data-collector

RUN make prod

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Modbus Data Collector written in Go"
LABEL version="2.4.2"

COPY --from=golang /data-collector .

ENTRYPOINT ["/data-collector"]