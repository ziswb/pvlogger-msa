FROM gradle:8-jdk21-alpine AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN gradle :pvlogger-modbus-data-accessor:clean :pvlogger-modbus-data-accessor:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Data Accessor"
LABEL version="3.7.5"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-modbus-data-accessor/build/libs/pvlogger-modbus-data-accessor-all.jar" "/opt/pvlogger/data-accessor.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/data-accessor.jar", \
    "-config=application-prod.conf" \
]
