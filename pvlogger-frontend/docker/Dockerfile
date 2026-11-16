# Stage 1: Angular SPA, native on the build host like the JAR stage. Layered
# so unchanged web sources are a full cache hit: the npm ci layer only
# invalidates on lockfile changes, test+build only on changes under src/web/.
FROM --platform=$BUILDPLATFORM node:26-alpine@sha256:6bd9ab43c0ec0190ca7b2d193906cb8b7edf6e7f4afa43ddeccd6452710bffb5 AS web

WORKDIR /app
COPY pvlogger-frontend/src/web/package.json pvlogger-frontend/src/web/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY pvlogger-frontend/src/web/ ./
RUN npm run build:ci

# Stage 2: fat JAR, with the SPA placed into the Ktor static resources
# Native on the build host: the fat JAR is platform-independent (multi-arch
# releases, ADR-0027); only the runtime stage is per platform.
FROM --platform=$BUILDPLATFORM gradle:9-jdk25-alpine@sha256:0ae8e5ac9d253704444bb8acb7f905928d09b2e5ac5e05c42737f74e261f6623 AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
COPY . /workspace
COPY --from=web /app/dist/web /workspace/pvlogger-frontend/src/main/resources/static

WORKDIR /workspace
# Dependency cache survives between builds (BuildKit, backlog 60).
RUN --mount=type=cache,target=/workspace/gradle_home/caches \
    gradle :pvlogger-frontend:buildFatJar

FROM eclipse-temurin:25-jre-ubi10-minimal@sha256:2e8ee3e5f7ee514713aaae37ccaa7902a1c24c0532dcc8712312d8a002214828

LABEL author="Bruno Ziswiler"
LABEL description="Frontend for PV Logger"
LABEL version="3.15.0"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-frontend/build/libs/pvlogger-frontend-all.jar" "/opt/pvlogger/frontend.jar"
# A neutral service user; uid 1000 as before (the chart sets its own uid too).
RUN useradd -u 1000 pvlogger
USER pvlogger

# DATA_ACCESSOR_HOST/PORT come from the chart; without them the app uses
# localhost:8083 (application-prod.conf).

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/frontend.jar", \
    "-config=application-prod.conf" \
]
