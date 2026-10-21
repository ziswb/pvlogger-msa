# 0022 — `fsdata` migrations: tern, embedded in fusionsolar, run as a manual `MIGRATE` job

- Status: accepted (2026-10-21)
- Date: 2026-10-21

## Context

`fsdata` has had no schema migrations since the Kotlin→Go rewrite of fusionsolar
(ADR-0002, open question 4). Its DDL was committed only as sqlc's input,
`schema.fsdata.sql`, kept by hand: nothing applied it to a database, nothing tied
it to production. Backlog item 29 step 1 verified that file against a
`pg_dump --schema-only` of the live `fsdata` (2026-10-20) and reconciled the one
real difference (`fs_response.raw` is `text`). CloudNativePG (infrastructure
ADR-0022) will copy `fsdata` with `initdb.import`; after that, the old server is
no longer around as a reference.

## Decision

1. **tern** (`github.com/jackc/tern/v2`, same author and driver as the pgx the
   module already uses) applies migrations from SQL files embedded with
   `go:embed` (`internal/database/migrations/NNN_name.sql`). No Flyway in Go, no
   CLI in the image.
2. **Inside `pvlogger-fusionsolar`**, as job `MIGRATE` in its job dispatch — no
   new service (ADR-0007). Migrations sit next to the sqlc queries that must
   match them. The job needs only the database settings; it runs before the
   FusionSolar token is fetched.
3. **Run manually** as a plain K8s Job from the same image and env as the
   CronJobs, never on a schedule and never implicitly before a collection job.
   tern's advisory lock serialises concurrent runs.
4. **The migrations are the schema.** sqlc reads `migrations/` as its schema
   input; `schema.fsdata.sql` is deleted, so the file sqlc checks the queries
   against and the DDL that reaches the database cannot drift apart.
5. **Baseline by idempotence:** `001_baseline.sql` is the verified schema with
   every `create` as `IF NOT EXISTS`. On the existing database it changes nothing
   and records version 1; on an empty one it builds the schema. Later migrations
   are plain DDL.
6. **History table `public.tern_schema_version`**, named so nobody mistakes it for
   the Kotlin era's `flyway_schema_history`, which stays untouched (its rows are
   the only record of the old migrations).

## Consequences

- A schema change is now: new `NNN_*.sql`, `sqlc generate`, release, run the
  `MIGRATE` Job, then deploy code that depends on it. The chart needs a manual
  Job template (infrastructure side).
- tern records only the version number, no checksum: editing an applied
  migration goes unnoticed. Applied migrations are never edited.
- The `IF NOT EXISTS` baseline would also "pass" a database that has an older
  table of the same name with different columns. Acceptable: the only database
  it meets with existing tables is production, which step 1 verified.
- pgx moves to 5.10.0 in fusionsolar (tern 2.4.3 requires it).
