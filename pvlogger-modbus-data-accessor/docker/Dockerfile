# Native on the build host: the fat JAR is platform-independent (multi-arch
# releases, ADR-0027); only the runtime stage is per platform.
FROM --platform=$BUILDPLATFORM gradle:9-jdk25-alpine@sha256:0ae8e5ac9d253704444bb8acb7f905928d09b2e5ac5e05c42737f74e261f6623 AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
COPY . /workspace

WORKDIR /workspace
# Dependency cache survives between builds (BuildKit, backlog 60).
RUN --mount=type=cache,target=/workspace/gradle_home/caches \
    gradle :pvlogger-modbus-data-accessor:clean :pvlogger-modbus-data-accessor:buildFatJar

FROM eclipse-temurin:25-jre-ubi10-minimal@sha256:2e8ee3e5f7ee514713aaae37ccaa7902a1c24c0532dcc8712312d8a002214828

LABEL author="Bruno Ziswiler"
LABEL description="PV Logger Data Accessor"
LABEL version="3.14.2"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-modbus-data-accessor/build/libs/pvlogger-modbus-data-accessor-all.jar" "/opt/pvlogger/data-accessor.jar"
# A neutral service user; uid 1000 as before (the chart sets its own uid too).
RUN useradd -u 1000 pvlogger
USER pvlogger

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/data-accessor.jar", \
    "-config=application-prod.conf" \
]
