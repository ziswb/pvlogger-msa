# Stage 1: Angular SPA. Layered so unchanged web sources are a full cache hit:
# the npm ci layer only invalidates on lockfile changes, test+build only on
# changes under src/web/.
FROM node:26-alpine@sha256:6bd9ab43c0ec0190ca7b2d193906cb8b7edf6e7f4afa43ddeccd6452710bffb5 AS web

WORKDIR /app
COPY pvlogger-frontend/src/web/package.json pvlogger-frontend/src/web/package-lock.json ./
RUN npm ci

COPY pvlogger-frontend/src/web/ ./
RUN npm run build:ci

# Stage 2: fat JAR, with the SPA placed into the Ktor static resources
FROM gradle:9-jdk25-alpine@sha256:0ae8e5ac9d253704444bb8acb7f905928d09b2e5ac5e05c42737f74e261f6623 AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace
COPY --from=web /app/dist/web /workspace/pvlogger-frontend/src/main/resources/static

WORKDIR /workspace
RUN gradle :pvlogger-frontend:buildFatJar

FROM eclipse-temurin:25-jre-ubi10-minimal@sha256:2e8ee3e5f7ee514713aaae37ccaa7902a1c24c0532dcc8712312d8a002214828

LABEL author="Bruno Ziswiler"
LABEL description="Frontend for PV Logger"
LABEL version="3.14.1"

RUN mkdir -p "/opt/pvlogger/"
COPY --from=build "/workspace/pvlogger-frontend/build/libs/pvlogger-frontend-all.jar" "/opt/pvlogger/frontend.jar"
RUN useradd -u 1000 bziswiler
USER bziswiler

ENV DATA_ACCESSOR_HOST="pvlogger-modbus-data-accessor-service.pvlogger.svc.cluster.local"
ENV DATA_ACCESSOR_PORT="8083"

ENTRYPOINT [ \
    "java", \
    "-jar", \
    "/opt/pvlogger/frontend.jar", \
    "-config=application-prod.conf" \
]
