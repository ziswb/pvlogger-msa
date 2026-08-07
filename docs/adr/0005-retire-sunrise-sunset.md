# 0005 — Retire sunrise-sunset: FusionSolar collection runs around the clock

- Status: accepted (recorded 2026-08-07 from the operator's answer to open question 11)
- Date: 2026-08-07

## Context

The FusionSolar collectors predate the Modbus collectors on the homelab.
`pvlogger-sunrise-sunset` existed to gate FusionSolar API collection to
daylight hours (start just before sunrise, stop just after sunset): at night
nothing is produced, the data was considered uninteresting, and skipping it
saved disk space.

## Decision

Both premises have reversed — nightly data has become of interest, and disk
space is a problem for later. FusionSolar collection is no longer gated by
daylight, and **`pvlogger-sunrise-sunset` is deprecated**. Nothing calls it
anymore (no code references, no Helm template; only its `sunrisesunset`
database lingers in the db-backup job).

## Consequences

- The module, its `sunrisesunset` database, and the database's entry in the
  chart's db-backup job can be removed — consolidation backlog item 28 here,
  infrastructure backlog item 32 for the chart/database side.
- Consolidation backlog item 27 (externalize the hard-coded site coordinates)
  is superseded: the coordinates disappear with the module.
