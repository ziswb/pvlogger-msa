# 0008 — Database name is stated once; the canonical JDBC URL is derived

- Status: accepted
- Date: 2026-08-07

## Context

`DatabaseConfig` required both `dbName` and `dbServerCanonicalUrl`, stating the
database name twice per config profile. A prod copy-paste slip
(`dbName = "fsdata"` vs. `/pvmasterdata` in the URL) stayed dormant for a long
time because `dbName` is only consumed by first-boot self-provisioning — the
exists-check/create step — while all real access uses the canonical URL
(consolidation backlog item 1).

## Decision

`dbServerCanonicalUrl` is **derived** in the shared `DatabaseConfig`:
`dbServerUrl + dbName + dbUrlParams`. HOCON profiles state the database name
once (`dbName`) and pass connection parameters via `dbUrlParams`. An explicit
`dbServerCanonicalUrl` remains supported as an **override**, solely for URL
schemes that don't compose this way (the H2 in-memory dev/test configs).

## Consequences

- The provisioning step and the real connection can no longer target different
  databases; the whole mismatch class is gone where PostgreSQL is used.
- New services get the safe derived behavior by default.
- The H2 override configs keep byte-identical URLs — no behavior change there.
- The deprecated sunrise-sunset module was deliberately left on the override
  path (it is scheduled for deletion, backlog item 28).
