FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-frontend:clean :pvlogger-frontend:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="Frontend for PV Logger"
LABEL version="1.0.0"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-frontend/build/libs/pvlogger-frontend-all.jar" "/opt/pvlogger/frontend.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV DATA_ACCESSOR_HOST="pvlogger-modbus-data-accessor-service.pvlogger.svc.cluster.local"
ENV DATA_ACCESSOR_PORT="8083"
ENV ACTIVEMQ_HOST="activemq.activemq.svc.cluster.local"
ENV ACTIVEMQ_PORT="61616"
ENV TZ="Europe/Zurich"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/frontend.jar", \
    "-config=application-prod.conf" \
]
