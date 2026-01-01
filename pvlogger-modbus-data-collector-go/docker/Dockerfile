FROM golang:1.25.4-alpine AS golang

WORKDIR /app
COPY ./pvlogger-modbus-data-collector-go/app/ .

RUN go mod download
RUN go mod verify
RUN CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o /data-collector .

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Modbus Data Collector written in Go"
LABEL version="2.2.4"

COPY --from=golang /data-collector .

ENTRYPOINT ["/data-collector"]