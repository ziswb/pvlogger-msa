FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-kpi-exporter:clean :pvlogger-kpi-exporter:build

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger KPI Exporter"
LABEL version="1.5.5"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-kpi-exporter/build/distributions/pvlogger-kpi-exporter.tar" "/opt/pvlogger/"
RUN tar -xvf "/opt/pvlogger/pvlogger-kpi-exporter.tar" -C "/opt/pvlogger/"
RUN microdnf -y update && microdnf install -y findutils
RUN useradd -u 1000 pvlogger
USER pvlogger

ENV TZ="Europe/Zurich"

ENTRYPOINT [ "/opt/pvlogger/pvlogger-kpi-exporter/bin/pvlogger-kpi-exporter" ]
