# 0009 — Data-accessor tests run against real PostgreSQL (Testcontainers)

- Status: accepted
- Date: 2026-08-07

## Context

The data-accessor tests ran on in-memory H2 in PostgreSQL mode. V6
(`UseTimestampWithTimezone`) is PostgreSQL-only syntax, so Flyway failed on H2
and all 22 tests were broken (consolidation backlog item 31). H2 compatibility
was an implicit requirement on every migration (V8 documents it) — a recurring
tax that V6 showed gets forgotten. Worse, H2 masked real divergence: it never
ran V6, so the tests asserted UTC day-bucketing in the daily-delta SQL while
production (TIMESTAMPTZ + `TZ=Europe/Zurich`) buckets by Zurich days.

## Decision

- Data-accessor tests run against a **real PostgreSQL 18** (the production
  version) via **Testcontainers**: one container per test JVM, the `pvdata`
  database self-provisioned by `DatabaseFactory` (ADR-0008 derived-URL path),
  migrated once by Flyway, tables truncated before each test. V8's cluster-wide
  `CREATE ROLE` is why tests share one database instead of getting one each.
- **`pvdata` migrations no longer need H2 compatibility.** (masterdata-server
  still tests on H2; its migrations must stay H2-compatible until it moves
  too.)
- Running the data-accessor tests requires a running Docker daemon (Rancher
  Desktop). Machine-side plumbing lives in `~/.testcontainers.properties`
  (`docker.host` → Rancher Desktop's socket); repo-side in the test task
  (`api.version` pin for Docker ≥ 29, in-VM socket override for Ryuk).

## Consequences

- Migrations and daily-delta SQL are verified against the database that runs
  in production; the H2-masking bug class is gone for this service.
- Timestamp test data must be built in the session timezone (the JVM zone),
  not UTC — `CAST(... AS DATE)`/`CURRENT_DATE` follow the session timezone.
- Test runs cost a few seconds of container startup per module run.
