FROM golang:1.25.4 AS golang

ADD . /
WORKDIR /pvlogger-kpi-exporter

RUN make prod

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger KPI Exporter"
LABEL version="3.4.1"

COPY --from=golang /exporter .

ENTRYPOINT ["/exporter"]