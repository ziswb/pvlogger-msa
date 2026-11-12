FROM golang:1.27.4 AS golang

ADD . /
WORKDIR /pv-device-data-collector

RUN make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PV Device Data Collector written in Go"
LABEL version="3.7.3"

COPY --from=golang /device-data-collector .

ENTRYPOINT ["/device-data-collector"]