# 0017 — Project-curated register definitions enter masterdata via Flyway migration; vendor CSVs stay import-only

- Status: accepted
- Date: 2026-08-14

## Context

Backlog item 35 (ADR-0016) needs the SmartDongle's unit-100 aggregate block in
the masterdata catalog so the full collector's sweep picks it up. The existing
ingestion path is a REST-triggered importer reading three Huawei SUN2000 CSV
exports from a volume outside the repo. The dongle block is not vendor data:
it is absent from Huawei's inverter interface PDF and CSV exports, hand-curated
from the `wlcrs/huawei-solar-lib` register table and live verification
(backlog 33), and its store/read types must stay in lock-step with
`pvlogger-shared-modbus/registers` — the definitions the shared KPI
aggregation reads values back by (ADR-0016).

Considered: a fourth hand-written CSV (puts repo-authored data on an external
volume, needs deployment changes and a manual import trigger), extending the
importer to read a classpath resource (mixed sources, still a manual trigger),
or a Flyway data migration.

## Decision

Register definitions the project curates itself ship as **Flyway migrations in
masterdata-server** (first: `V2__DongleAggregateBlock.sql`), idempotent against
the unique address index. The CSV importer remains exclusively for Huawei's
own register exports. A drift test in `pvlogger-shared-modbus/registers`
parses the migration and fails the Go build if it diverges from the shared
definitions (addresses, quantities, and the types the per-type read-back
depends on) — the same source-parsing pattern as the REST-path drift test.

## Consequences

- Curated catalog data is versioned with the code that depends on it and
  deploys itself with the next masterdata-server image — no volume changes,
  no import call; deploy masterdata-server before (re)enabling the full
  collector.
- Re-running the CSV import cannot clash with migration rows (batch insert
  ignores the unique address index) and vice versa (`on conflict do nothing`).
- Masterdata tests (Testcontainers, ADR-0009) exercise every migration on
  startup, so a broken data migration fails the build, not the rollout.
- The catalog records the device truth (37516: W, gain 1); the deliberate Go
  deviation for the raw-row column convention (kW/THOUSAND) stays documented
  in `register-definitions.go` and is exempted in the drift test.
