FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-kpi-exporter:clean :pvlogger-kpi-exporter:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger KPI Exporter"
LABEL version="1.4.10"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-kpi-exporter/build/libs/pvlogger-kpi-exporter-all.jar" "/opt/pvlogger/kpi-exporter.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV TZ="Europe/Zurich"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/kpi-exporter.jar", \
    "-config=application-prod.conf" \
]
