FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-modbus-data-collector:clean :pvlogger-modbus-data-collector:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Data Collector"
LABEL version="1.1.0"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-modbus-data-collector/build/libs/pvlogger-modbus-data-collector-all.jar" "/opt/pvlogger/data-collector.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV TZ="Europe/Zurich"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/data-collector.jar", \
    "-config=application-prod.conf" \
]
