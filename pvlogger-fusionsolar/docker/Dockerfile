FROM golang:1.27.4 AS golang

ADD . /
WORKDIR /pvlogger-fusionsolar

RUN make prod

FROM gcr.io/distroless/static-debian13

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Fusion Solar"
LABEL version="3.5.3"

COPY --from=golang /fusionsolar .

ENTRYPOINT ["/fusionsolar"]