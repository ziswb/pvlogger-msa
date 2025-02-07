FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-heartbeat:clean :pvlogger-heartbeat:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="Job Trigger for PV Logger"
LABEL version="1.3.1"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-heartbeat/build/libs/pvlogger-heartbeat-all.jar" "/opt/pvlogger/heartbeat.jar"
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
    "/opt/pvlogger/heartbeat.jar", \
    "-config=application-prod.conf" \
]
