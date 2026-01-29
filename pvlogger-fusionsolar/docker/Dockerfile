FROM golang:1.25.4 AS golang

ADD . /app
WORKDIR /app/pvlogger-fusionsolar/app

RUN make prod

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Fusion Solar"
LABEL version="2.1.0"

COPY --from=golang /fusionsolar .

ENTRYPOINT ["/fusionsolar"]