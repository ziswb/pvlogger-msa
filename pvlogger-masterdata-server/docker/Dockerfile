FROM gradle:9-jdk25-alpine AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN gradle :pvlogger-masterdata-server:clean :pvlogger-masterdata-server:buildFatJar

FROM eclipse-temurin:25-jre-ubi10-minimal

LABEL author="Bruno Ziswiler"
LABEL description="Backend Image for PV Logger"
LABEL version="3.6.0"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-masterdata-server/build/libs/pvlogger-masterdata-server-all.jar" "/opt/pvlogger/masterdata-server.jar"
COPY "pvlogger-masterdata-server/docker/Sun2000*.csv" "/opt/pvlogger/masterdata/"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/masterdata-server.jar", \
    "-config=application-prod.conf" \
]
