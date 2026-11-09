FROM golang:1.27.3 AS golang

ADD . /
WORKDIR /pvlogger-fusionsolar

RUN make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Fusion Solar"
LABEL version="3.5.2"

COPY --from=golang /fusionsolar .

ENTRYPOINT ["/fusionsolar"]