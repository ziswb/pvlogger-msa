FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-modbus-data-collector:clean :pvlogger-modbus-data-collector:shadowJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Data Collector"
LABEL version="1.0.0"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-modbus-data-collector/build/libs/pvlogger-modbus-data-collector-all.jar" "/opt/pvlogger/data-collector.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV PG_HOST="postgres-service.postgresql.svc.cluster.local"
ENV PG_PORT="5432"
ENV MQ_HOST="activemq.activemq.svc.cluster.local"
ENV MQ_PORT="61616"
ENV TZ="Europe/Zurich"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/data-collector.jar", \
    "-config=application-prod.conf" \
]
