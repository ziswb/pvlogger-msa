FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-modbus-data-collector:clean :pvlogger-modbus-data-collector:build

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Data Collector"
LABEL version="1.5.0"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-modbus-data-collector/build/distributions/pvlogger-modbus-data-collector.tar" "/opt/pvlogger/"
RUN tar -xvf "/opt/pvlogger/pvlogger-modbus-data-collector.tar" -C "/opt/pvlogger/"
RUN microdnf -y update && microdnf install -y findutils
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV TZ="Europe/Zurich"

ENTRYPOINT [ "/opt/pvlogger/pvlogger-modbus-data-collector/bin/pvlogger-modbus-data-collector" ]
