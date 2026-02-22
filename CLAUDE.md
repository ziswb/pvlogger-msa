# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PVLogger is a solar panel monitoring and data collection system built as a polyglot microservices architecture. It integrates with Modbus devices and FusionSolar APIs, stores data in PostgreSQL, and serves a dashboard frontend.

## Build Commands

### JVM Services (Gradle)

```bash
./gradlew build                              # Build all JVM services
./gradlew test                               # Run all JVM tests
./gradlew :pvlogger-masterdata-server:test   # Run tests for a single service
./gradlew :pvlogger-modbus-data-accessor:test
```

### Go Services (Make via Gradle)

```bash
./gradlew makeGoBuild   # Build all Go services
./gradlew makeGoLint    # Lint all Go code
```

Go modules are managed via `go.work` at the root. Individual Go services can be built directly:

```bash
cd pvlogger-modbus-data-collector && make build
cd pvlogger-fusionsolar && make build
```

### Local Development

```bash
tilt up     # Start full local K8s dev environment (requires Docker Desktop K8s)
tilt down   # Tear down
```

Database runs via Docker Compose in `pvlogger-database/`.

## Architecture

### Service Map

**JVM Services (Kotlin/Ktor):**
- `pvlogger-frontend` — Angular 15 SPA served by Ktor (port 8080). Dashboard with ag-grid tables and Highcharts visualizations.
- `pvlogger-masterdata-server` — Device metadata CRUD service (port 8081)
- `pvlogger-modbus-data-accessor` — Central data store/query service for register values and KPIs (port 8083)
- `pvlogger-modbus-mock-server` — Mock Modbus device for local testing
- `pvlogger-sunrise-sunset` — Sunrise/sunset time calculation
- `pvlogger-shared` — Shared Kotlin library (REST path constants, serialization models, database utilities)

**Go Services (CronJob-based):**
- `pvlogger-modbus-data-collector` — Scheduled Modbus polling, stores via Data Accessor REST API
- `pvlogger-fusionsolar` — FusionSolar API data integration
- `pvlogger-fusionsolar-auth` — FusionSolar authentication handling
- `pvlogger-kpi-exporter` — KPI data export
- `pv-logger` — Shared Go library (logging)

### Communication Flow

```
Modbus Devices → modbus-data-collector (Go CronJob) → modbus-data-accessor (REST POST)
FusionSolar API → fusionsolar (Go CronJob) → modbus-data-accessor (REST POST)
Frontend → modbus-data-accessor (REST GET for live data, KPIs)
Frontend → masterdata-server (REST GET for device metadata)
```

Kubernetes DNS handles inter-service discovery. Traefik ingress routes `/v1*` to Data Accessor and `/pv*`, `/livedata*` to Frontend.

### Data Storage

PostgreSQL 17.2 with Flyway migrations (in each service's `src/main/resources/db/migration/`). Exposed ORM for Kotlin services, pgx driver for Go services.

## Code Conventions

### JVM Service Structure

Each Kotlin service follows this layout:
```
src/main/kotlin/ch/bziswiler/pvlogger/<service>/
  Application.kt              # Entry point
  configuration/               # Koin DI modules, Ktor plugins (routing, serialization, monitoring)
  database/access/             # DAO classes using Exposed
  database/schema/             # Table definitions using Exposed
  rest/                        # Ktor route handlers
  service/                     # Business logic
  model/                       # Kotlinx Serialization data classes
```

Configuration uses HOCON files: `application-prod.conf`, `application-dev.conf`, `application-test.conf` with environment variable interpolation (`${ENV_VAR}`).

### Go Service Structure

Go services use a job dispatch pattern (switch on job name), read K8s secrets for credentials, and import shared code from `pv-logger` via workspace replace directives.

### Key Technology Choices

- **Ktor 2.x** for HTTP (not Spring) — async, lightweight
- **Koin** for DI (not Dagger/Hilt)
- **Exposed** for database access (not JPA/Hibernate)
- **Kotlinx Serialization** (not Jackson/Gson)
- **JVM 21** toolchain target
- **Go 1.25** for CronJob services
- Docker images: JVM uses `eclipse-temurin:21-jre-ubi9-minimal`, Go uses `gcr.io/distroless/static-debian12`

### Dependency Versions

Centralized in `gradle/libs.versions.toml`. Key versions are managed there — always update the version catalog rather than individual build files.

### Testing

JVM tests use Ktor test host, Koin test, and JUnit 5. Test utilities live in `testutils` packages. Tests use H2 in-memory database via `application-test.conf`.
