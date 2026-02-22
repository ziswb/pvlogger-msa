FROM golang:1.25.4 AS golang

ADD . /
WORKDIR /pv-dynamic-data-collector

RUN make prod

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Dynamic Data Collector - reads all dynamic Modbus register values on demand"
LABEL version="0.1.0"

COPY --from=golang /dynamic-data-collector .

ENTRYPOINT ["/dynamic-data-collector"]
