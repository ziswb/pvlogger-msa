FROM golang:1.25.4 AS golang

ADD . /
WORKDIR /pvlogger-fusionsolar-auth

RUN make prod

FROM gcr.io/distroless/static-debian12

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger FusionSolar Auth Service"
LABEL version="1.0.0"

COPY --from=golang /fusionsolar-auth .

ENTRYPOINT ["/fusionsolar-auth"]
