FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-modbus-data-accessor:clean :pvlogger-modbus-data-accessor:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Data Accessor"
LABEL version="1.1.3"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-modbus-data-accessor/build/libs/pvlogger-modbus-data-accessor-all.jar" "/opt/pvlogger/data-accessor.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV POSTGRESQL_HOST="postgres-service.postgresql.svc.cluster.local"
ENV POSTGRESQL_PORT="5432"
ENV ACTIVEMQ_HOST="activemq.activemq.svc.cluster.local"
ENV ACTIVEMQ_PORT="61616"
ENV TZ="Europe/Zurich"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/data-accessor.jar", \
    "-config=application-prod.conf" \
]
