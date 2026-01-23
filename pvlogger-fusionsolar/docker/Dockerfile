FROM golang:1.25.4-alpine AS golang

ADD . /app
WORKDIR /app/pvlogger-fusionsolar/app

RUN go mod download
RUN go mod verify
RUN CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o /fusionsolar .

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Fusion Solar"
LABEL version="2.1.0"

COPY --from=golang /fusionsolar .

ENTRYPOINT ["/fusionsolar"]