FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-crepuscular-service:clean :pvlogger-crepuscular-service:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="Sunrise/Sunset Service for PV Logger"
LABEL version="1.0.0"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-crepuscular-service/build/libs/pvlogger-crepuscular-service-all.jar" "/opt/pvlogger/crepuscular-service.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV TZ="Europe/Zurich"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/crepuscular-service.jar", \
    "-config=application-prod.conf" \
]
