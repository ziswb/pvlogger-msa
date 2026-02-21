FROM golang:1.25.4 AS golang

ADD . /
WORKDIR /pv-data-collector

RUN make prod

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Data Collector - focused KPI register reader and KPI computer"
LABEL version="0.1.4"

COPY --from=golang /data-collector .

ENTRYPOINT ["/data-collector"]