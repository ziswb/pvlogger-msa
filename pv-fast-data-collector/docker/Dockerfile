FROM golang:1.27.4 AS golang

ADD . /
WORKDIR /pv-fast-data-collector

RUN make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PV Fast Data Collector - focused KPI register reader and KPI computer"
LABEL version="0.13.3"

COPY --from=golang /fast-data-collector .

ENTRYPOINT ["/fast-data-collector"]