FROM golang:1.25.4 AS golang

ADD . /
WORKDIR /pv-device-data-collector

RUN make prod

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Device Data Collector written in Go"
LABEL version="3.2.0"

COPY --from=golang /device-data-collector .

ENTRYPOINT ["/device-data-collector"]