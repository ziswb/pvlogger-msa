FROM gradle:9-jdk25-alpine AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN gradle :pvlogger-modbus-mock-server:clean :pvlogger-modbus-mock-server:buildFatJar

FROM eclipse-temurin:25-jre-ubi10-minimal

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Modbus Mock Data"
LABEL version="3.2.2"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-modbus-mock-server/build/libs/pvlogger-modbus-mock-server-all.jar" "/opt/pvlogger/mock-server.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/mock-server.jar", \
    "-config=application-prod.conf" \
]
