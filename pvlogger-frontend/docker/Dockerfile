# Stage 1: Angular SPA. Layered so unchanged web sources are a full cache hit:
# the npm ci layer only invalidates on lockfile changes, test+build only on
# changes under src/web/.
FROM node:22-alpine AS web

RUN apk add --no-cache chromium
ENV CHROME_BIN=/usr/bin/chromium-browser

WORKDIR /app
COPY pvlogger-frontend/src/web/package.json pvlogger-frontend/src/web/package-lock.json ./
RUN npm ci

COPY pvlogger-frontend/src/web/ ./
RUN npm run build:ci

# Stage 2: fat JAR, with the SPA placed into the Ktor static resources
FROM gradle:8-jdk21-alpine AS build

ENV GRADLE_USER_HOME="/workspace/gradle_home"
ADD . /workspace
COPY --from=web /app/dist/web /workspace/pvlogger-frontend/src/main/resources/static

WORKDIR /workspace
RUN gradle :pvlogger-frontend:buildFatJar

FROM eclipse-temurin:21-jre-ubi9-minimal

LABEL author="Bruno Ziswiler"
LABEL description="Frontend for PV Logger"
LABEL version="3.11.0"

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
