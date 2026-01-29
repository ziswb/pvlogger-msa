FROM golang:1.25.4 AS golang

ADD . /app
WORKDIR /app/pvlogger-kpi-exporter/app

RUN make prod

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger KPI Exporter"
LABEL version="2.0.5"

COPY --from=golang /exporter .

ENTRYPOINT ["/exporter"]