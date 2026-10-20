FROM golang:1.27.3 AS golang

ADD . /
WORKDIR /pv-full-data-collector

RUN make prod

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Full Data Collector - reads all dynamic Modbus register values on demand"
LABEL version="0.8.2"

COPY --from=golang /full-data-collector .

ENTRYPOINT ["/full-data-collector"]
