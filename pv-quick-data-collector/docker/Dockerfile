FROM golang:1.25.4 AS golang

ADD . /
WORKDIR /pv-quick-data-collector

RUN make prod

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Quick Data Collector - focused KPI register reader and KPI computer"
LABEL version="0.3.0"

COPY --from=golang /quick-data-collector .

ENTRYPOINT ["/quick-data-collector"]