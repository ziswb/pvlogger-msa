FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-fusionsolar:clean :pvlogger-fusionsolar:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="Data Collector from Huawei Fusion Solar Cloud"
LABEL version="1.4.10"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-fusionsolar/build/libs/pvlogger-fusionsolar-all.jar" "/opt/pvlogger/fusionsolar.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV TZ="Europe/Zurich"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/fusionsolar.jar", \
    "-config=application-prod.conf" \
]
