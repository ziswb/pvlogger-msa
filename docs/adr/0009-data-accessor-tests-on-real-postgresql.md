# 0009 — JVM tests run against real PostgreSQL (Testcontainers); H2 removed

- Status: accepted
- Date: 2026-08-07

## Context

DB-touching JVM tests ran on in-memory H2 in PostgreSQL mode, which imposed an
implicit H2-compatibility requirement on every migration (V8 documents it).
The data-accessor's V6 (`UseTimestampWithTimezone`) is PostgreSQL-only syntax,
so Flyway failed on H2 and all 22 of its tests were broken (consolidation
backlog item 31). Worse, H2 masked real divergence: it never ran V6, so a test
asserted UTC day-bucketing in the daily-delta SQL while production
(TIMESTAMPTZ + `TZ=Europe/Zurich`) buckets by Zurich days.

## Decision

- DB-touching tests (data-accessor, masterdata-server) run against a **real
  PostgreSQL 18** (the production version) via **Testcontainers**: one
  container per test JVM, the database self-provisioned by `DatabaseFactory`
  (ADR-0008 derived-URL path), migrated once by Flyway, tables truncated
  before each test. V8's cluster-wide `CREATE ROLE` is why tests share one
  database instead of getting one each.
- **H2 is removed entirely** — dependency, version-catalog entry, and the
  `application-dev.conf` profiles, which now point at the local
  `pvlogger-database` Docker Compose PostgreSQL (`localhost:5432`, same target
  as `application-test.conf`).
- With no H2 left, `DatabaseConfig` needs no canonical-URL override: the URL
  is **always derived** (`dbServerUrl + dbName + dbUrlParams`), completing
  ADR-0008. The dead quartz-era `driverDelegateClass` field was dropped in the
  same pass.
- **Migrations no longer need H2 compatibility — for any service.**
- Running DB-touching tests requires a running Docker daemon (Rancher
  Desktop). Machine-side plumbing lives in `~/.testcontainers.properties`
  (`docker.host` → Rancher Desktop's socket); repo-side in the test tasks
  (in-VM socket override for Ryuk; the `api.version` pin for Docker ≥ 29
  was dropped with Testcontainers 2, which negotiates the API version itself).

## Consequences

- Migrations and raw SQL are verified against the database that runs in
  production; the H2-masking bug class is gone.
- Timestamp test data must be built in the session timezone (the JVM zone),
  not UTC — `CAST(... AS DATE)`/`CURRENT_DATE` follow the session timezone.
- Test runs cost a few seconds of container startup per module; dev mode
  (`run-server.sh`) requires the compose database to be running.
