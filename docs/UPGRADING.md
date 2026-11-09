# Upgrading dependencies and tooling

This is the standing procedure for "update everything" requests, so it doesn't need to be agreed on each time.
It covers libraries, major versions and tooling (JDK, Go, Gradle, Node/Angular, base images).

## Ground rules

- Work on a branch. Nothing is pushed, merged or deployed without the owner's go-ahead.
  Rollout happens through the sibling `infrastructure` repo, and the owner watches the first CronJob runs.
- **Major versions and toolchain changes need approval first.** Before any work, present a table with
  one row per major/toolchain bump: from → to, pros, cons/breaking changes, risk (low/med/high),
  and the effort expected. The owner picks which ones go ahead. Patch and minor bumps don't need approval.
- One concern per commit: patch/minor bumps grouped per ecosystem, each major on its own.
  This keeps `git bisect` and reverting cheap.
- Versions for the JVM services live only in `gradle/libs.versions.toml` (see CLAUDE.md).
- Follow the ADRs. An upgrade is not the moment to bring in new patterns. If an upgrade forces one,
  write it up as an ADR.

## Where versions live

| Area | Location |
|---|---|
| JVM libraries + Kotlin plugin | `gradle/libs.versions.toml` |
| Gradle | `gradle/wrapper/gradle-wrapper.properties` (`./gradlew wrapper --gradle-version X`) |
| JDK | Gradle toolchain config, `FROM gradle:*-jdk21-*` and `eclipse-temurin:21-*` in `*/docker/Dockerfile`, local `JAVA_HOME` |
| Go libraries | every `*/go.mod` (and `go.work.sum`) |
| Go toolchain | `go.work`, each `go.mod` `go` line, `FROM golang:*` in Dockerfiles, `.golangci.yml` / golangci-lint version |
| Frontend | `pvlogger-frontend/src/web/package.json` (+ lockfile) |
| Runtime images | `distroless/static-debian12`, `eclipse-temurin:*-ubi9-minimal` |
| Test/dev DB | Testcontainers `postgres:18` (3 test bases), `pvlogger-database/docker/docker-compose.yaml`, `pvlogger-database/scripts/run-db-container.sh` |

Not in scope here: the CNPG/PostgreSQL server version and the cluster setup. Those belong to the `infrastructure` repo.
The test and dev Postgres versions should match production's major version.

## Procedure

1. **Baseline (before changing anything)**
   - `JAVA_HOME` set to JDK 21 (or the current toolchain), Rancher Desktop running (Testcontainers).
   - `./gradlew build test`, `./gradlew makeGoBuild makeGoLint`, `go test ./...` in each Go module,
     `npm ci && npm run build` in `pvlogger-frontend/src/web`.
   - Write down tests that already fail, so they aren't blamed on the upgrade.
   - Capture reference outputs (against the local compose DB with the same data):
     `/v1/daydata` responses, a `/livedata` message, kpi-exporter `pv_*` JSON, the `/metrics` metric names.
2. **Inventory**: `./gradlew dependencyUpdates` (or check the catalog by hand), `go list -m -u all` per module,
   `npm outdated`, the latest tags of the base images. Sort into patch/minor and major/toolchain.
3. **Proposal**: the major/toolchain table from the ground rules. Wait for approval.
4. **Patch/minor upgrades**, per ecosystem, with a full test run after each.
5. **Approved majors**, one at a time: read the changelog/migration guide, upgrade, fix, test, commit.
   Angular goes one major at a time with `ng update`. Toolchain bumps update every place in the table above together.
6. **Behaviour checks**. These go beyond "tests pass" because they break quietly:
   - kpi-exporter JSON is identical to the baseline (the receiver's relay depends on it, ADR-0021).
   - REST/WebSocket JSON is unchanged (kotlinx-serialization defaults, how nulls and defaults are written).
   - Prometheus metric names are unchanged (Micrometer / Go client upgrades can rename them).
   - DB connection timeouts/keepalive still apply (backlog 45), TLS to the DB still works (ADR-0023).
   - Flyway migrations apply cleanly on a fresh PG; the daily-delta SQL timezone tests pass.
   - Every Docker image builds and its container starts.
   - Services run locally against the compose DB, and their output matches the baselines.
7. **Report**: what changed (from → to), what was skipped and why, how each item was tested, what can only
   be checked in production (real Modbus device, FusionSolar API, K8s deploy), and the rollout and rollback notes
   for the infrastructure repo.

## Known gaps

- There's no end-to-end local environment: Tilt and the mock server are broken (backlog 23, 30).
  Either repair them first as a separate step, or accept that the collector → data accessor path is only checked
  by unit tests and the first production run.
- The local dev DB versions (compose 17.2, script 16.2) are behind production's 18. Align them as part of the next upgrade.
