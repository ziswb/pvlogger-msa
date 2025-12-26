FROM golang:1.25.4-alpine AS golang

WORKDIR /app
COPY ./pvlogger-kpi-exporter/app/ .

RUN go mod download
RUN go mod verify
RUN CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o /exporter .

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger KPI Exporter"
LABEL version="2.0.5"

COPY --from=golang /exporter .

ENTRYPOINT ["/exporter"]