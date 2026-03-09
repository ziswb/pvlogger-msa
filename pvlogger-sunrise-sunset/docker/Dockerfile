FROM gradle:8-jdk21-alpine AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN gradle :pvlogger-sunrise-sunset:clean :pvlogger-sunrise-sunset:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="Sunrise/Sunset Service for PV Logger"
LABEL version="3.1.1"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-sunrise-sunset/build/libs/pvlogger-sunrise-sunset-all.jar" "/opt/pvlogger/sunrise-sunset.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV TZ="Europe/Zurich"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/sunrise-sunset.jar", \
    "-config=application-prod.conf" \
]
