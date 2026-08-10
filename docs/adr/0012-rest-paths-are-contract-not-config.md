# 0012 — REST paths are API contract constants, not deployment config

- Status: accepted
- Date: 2026-08-10

## Context

The REST paths of the data-accessor and masterdata APIs existed in three
places: the Kotlin route constants (`pvlogger-shared-rest/DataAccessorPaths.kt`,
masterdata-server's `MasterDataPaths.kt`) from which the servers compile their
routes; Go-side env-var defaults (`DA_*_PATH`, `MD_PATH`) in
`pvlogger-shared-modbus/config`; and the infrastructure repo, where the Helm
chart injected those env vars from Ansible defaults that restated the same
strings per collector (consolidation backlog item 9). In production the Go
defaults were dead — the Ansible copies were the live values, three hops from
the code. The env indirection offered no real flexibility: the server routes
are compiled in, so overriding a client's path env var can only break it.
A generated cross-language source of truth was considered and rejected as
over-machinery for two consumers (same bar as ADR-0007).

## Decision

REST paths are **API contract, stated once per language as constants — never
env-configurable**. The Kotlin route constants are the source of truth (the
servers derive their routes from them). The Go side mirrors them as exported
constants in `pvlogger-shared-modbus/config`; a drift test
(`config/paths_drift_test.go`) parses the Kotlin sources — same repo — and
fails the Go build if the values diverge. Host, port, and protocol remain env
config (`DA_HOST`, `MD_HOST`, …): where a service runs is deployment-specific,
what it serves is not. The `DA_*_PATH`/`MD_PATH` env plumbing is deleted from
the chart and Ansible defaults.

## Consequences

- Two copies remain (one per language), guarded by a build-time drift test
  instead of a codegen pipeline.
- ~50 lines of Ansible/Helm/env plumbing deleted; misconfiguring a path via
  values files is no longer possible.
- Adding an endpoint means: Kotlin constant + route, Go constant, drift-test
  mapping entry.
- The masterdata drift test reads masterdata-server's `MasterDataPaths.kt`;
  when that object moves to `pvlogger-shared-rest` (backlog item 18), the
  test's path must follow.
