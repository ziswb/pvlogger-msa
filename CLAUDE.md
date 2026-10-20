# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Read first:** [CONTEXT.md](CONTEXT.md) (purpose, domain vocabulary, component map, data flow),
[docs/adr/](docs/adr/) (decisions — check before introducing a new pattern),
[docs/CONSOLIDATION-BACKLOG.md](docs/CONSOLIDATION-BACKLOG.md) (known divergences — don't imitate them),
[docs/OPEN-QUESTIONS.md](docs/OPEN-QUESTIONS.md) (undocumented knowledge),
[docs/SECURITY-FINDINGS.md](docs/SECURITY-FINDINGS.md) (open security findings — note this
system has no authentication anywhere; network isolation is the only boundary).
Deployment lives in the sibling `infrastructure` repo (see `infrastructure/docs/LANDSCAPE.md`).

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
cd pv-device-data-collector && make build
cd pvlogger-fusionsolar && make build
```

### Local Development

```bash
tilt up     # Start full local K8s dev environment (requires Docker Desktop K8s)
tilt down   # Tear down
```

⚠️ Tilt (with the mock server) is the intended environment for developing larger
changes, but it has rotted: the Tiltfile references a `pvlogger-infrastructure/`
directory that no longer exists and has no `docker_build` steps, and the mock
server is outdated. Verify before relying on it — repair is consolidation-backlog
item 23, the mock-server refresh item 30 (both low priority).

Database runs via Docker Compose in `pvlogger-database/`.

## Architecture

### Service Map

**JVM Services (Kotlin/Ktor):**
- `pvlogger-frontend` — Angular 19 SPA (Angular Material + Highcharts) served by Ktor (port 8085); also serves the `/livedata` WebSocket
- `pvlogger-masterdata-server` — Modbus register master data CRUD/import service (port 8081)
- `pvlogger-modbus-data-accessor` — Central data store/query service for register values and KPIs (port 8083)
- `pvlogger-modbus-mock-server` — Mock Modbus TCP device for local testing (raw TCP port 8502)
- `pvlogger-shared-model`, `pvlogger-shared-rest`, `pvlogger-shared-database`, `pvlogger-shared-ktor` — Shared Kotlin libraries (Modbus domain model; REST path constants + KPI DTO; Hikari/Flyway bootstrap; Ktor service bootstrap: CallLogging/CallId, Prometheus `/metrics`, StatusPages, shared `PvHttpClient`, `closeOnShutdown` — ADR-0018)

**Go Services (CronJob-based):**
- `pv-fast-data-collector` — Per-minute Modbus polling of the hard-coded KPI register set; KPIs are derived by the shared `aggregate(jobId)` path (ADR-0016)
- `pv-full-data-collector` — Masterdata-driven extended register sweep, same shared KPI aggregation (hourly, see CONTEXT.md)
- `pv-device-data-collector` — Nightly static/config register + device identifier collection
- `pvlogger-fusionsolar` — FusionSolar API data integration (writes its own `fsdata` DB directly)
- `pvlogger-fusionsolar-auth` — Refreshes the FusionSolar token into a K8s secret
- `pvlogger-kpi-exporter` — Exports latest KPIs as JSON files (OneDrive-synced volume)
- `pv-logger`, `pvlogger-shared-modbus` — Shared Go libraries (logging/env config; Modbus TCP client, KPI math + single KPI aggregation path, hard-coded KPI register set, REST clients, collector jobs, Pushgateway metrics)

**Tools (not deployed):** `pvlogger-battery-simulator` (local CLI, see its README), `pvlogger-database` (local compose DB + migration history).

### Communication Flow

See CONTEXT.md for the full diagram. Short version:

```
Modbus device → fast/full/device collectors (Go CronJobs) → modbus-data-accessor (REST POST)
FusionSolar API → fusionsolar (Go CronJob) → fsdata DB (direct SQL, not via data accessor)
Angular SPA → /v1/daydata via ingress → modbus-data-accessor (history)
Angular SPA → /livedata WebSocket → frontend Ktor → modbus-data-accessor (latest KPI, 60s poll)
```

Kubernetes DNS handles inter-service discovery. Traefik ingress routes `/v1` to Data Accessor and `/`, `/livedata` to Frontend.

### Data Storage

PostgreSQL (18 in production, on the homelab central server) with Flyway migrations (in each service's `src/main/resources/db/migration/`; exception: `fsdata` has no migrations). Exposed ORM for Kotlin services, pgx/sqlc for Go services.

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

Configuration uses HOCON files with environment variable interpolation (`${ENV_VAR}`): `application-prod.conf` always, `application-dev.conf` where a local run differs from prod (points at the local Docker Compose DB). There are no `application-test.conf` files — JVM tests build their config programmatically (Testcontainers ports are dynamic, ADR-0009).

### Go Service Structure

Go services use a job dispatch pattern (switch on job name), read K8s secrets for credentials, and import shared code from `pv-logger` via workspace replace directives.

### Key Technology Choices

- **Ktor 3.x** for HTTP (not Spring) — async, lightweight
- **Koin** for DI (not Dagger/Hilt)
- **Exposed** for database access (not JPA/Hibernate)
- **Kotlinx Serialization** (not Jackson/Gson)
- **JVM 21** toolchain target
- **Go 1.27** for CronJob services
- Docker images: JVM uses `eclipse-temurin:21-jre-ubi9-minimal`, Go uses `gcr.io/distroless/static-debian12`

### Dependency Versions

Centralized in `gradle/libs.versions.toml`. Key versions are managed there — always update the version catalog rather than individual build files.

### Testing

JVM tests use Ktor test host, Koin test, and JUnit 5. Test utilities live in `testutils` packages.

- DB-touching tests (modbus-data-accessor, masterdata-server) run against a real PostgreSQL 18 via Testcontainers (ADR-0009) — a Docker daemon (Rancher Desktop) must be running. **There is no H2 anywhere; migrations never need H2 compatibility.**
- The dev profiles (`application-dev.conf`) point at the local `pvlogger-database` Docker Compose PostgreSQL (`localhost:5432`).
- Timestamp test data for the daily-delta SQL must be built in the JVM's zone, not UTC — `CAST(... AS DATE)`/`CURRENT_DATE` follow the session timezone.
