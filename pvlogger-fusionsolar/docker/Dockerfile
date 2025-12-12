FROM eclipse-temurin:21-jdk AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace

WORKDIR /workspace
RUN ./gradlew :pvlogger-fusionsolar:clean :pvlogger-fusionsolar:build

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="Data Collector from Huawei Fusion Solar Cloud"
LABEL version="1.5.9"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-fusionsolar/build/distributions/pvlogger-fusionsolar.tar" "/opt/pvlogger/"
RUN tar -xvf "/opt/pvlogger/pvlogger-fusionsolar.tar" -C "/opt/pvlogger/"
RUN microdnf -y update && microdnf install -y findutils
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV TZ="Europe/Zurich"

ENTRYPOINT [ "/opt/pvlogger/pvlogger-fusionsolar/bin/pvlogger-fusionsolar" ]
