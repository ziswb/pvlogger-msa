FROM golang:1.27.4 AS golang

ADD . /
WORKDIR /pv-full-data-collector

RUN make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PV Full Data Collector - reads all dynamic Modbus register values on demand"
LABEL version="0.8.3"

COPY --from=golang /full-data-collector .

ENTRYPOINT ["/full-data-collector"]
