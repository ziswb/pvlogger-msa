FROM gradle:8-jdk21-alpine AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN gradle :pvlogger-kpi-aggregator:clean :pvlogger-kpi-aggregator:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger KPI Aggregator"
LABEL version="1.4.11"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-kpi-aggregator/build/libs/pvlogger-kpi-aggregator-all.jar" "/opt/pvlogger/kpi-aggregator.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV TZ="Europe/Zurich"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/kpi-aggregator.jar", \
    "-config=application-prod.conf" \
]
