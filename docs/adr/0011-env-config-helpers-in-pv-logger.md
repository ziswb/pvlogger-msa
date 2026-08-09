# 0011 — Env-config helpers live in pv-logger/config and panic (Must-style)

- Status: accepted
- Date: 2026-08-09

## Context

Three copies of the same env-reading helpers existed: the shared
`pv-logger/config/env.go` (used by the collectors and pvlogger-shared-modbus),
a re-implementation in `pvlogger-kpi-exporter/internal/config/env.go`, and
private `panic()`-based variants in both fusionsolar modules (consolidation
backlog item 6). The copies had already drifted: the shared helpers called
`logging.Fatal` (untestable — `os.Exit` kills the test process), the
fusionsolar ones panicked (testable via `recover()`, with ~10 tests relying
on that), and fusionsolar-auth's `getEnv` treated a set-but-empty variable
as set while every other copy treated it as missing.

## Decision

All Go services read env configuration through `pv-logger/config`. The shared
helpers are **Must-style: they panic** on missing required variables or
unparseable values — not `logging.Fatal` — because startup-config validation
should be unit-testable and panics run deferred cleanup. This narrows the
`Fatal` exception documented in ADR-0010: startup-config helpers now panic;
`logging.Fatal` remains only in `cmd/` mains and job-dispatch layers.
`GetEnvOptional` (fusionsolar's conditionally-required password) moved into
the shared package. The helpers get their own test suite in
`pv-logger/config/env_test.go`; per-service config tests cover only their
`Load()`.

## Consequences

- One place to fix env-parsing behavior; the duplicate files are deleted.
- Behavior changes on misconfiguration only: exit code 2 with a panic trace
  instead of a structured log line (collectors), and fusionsolar-auth now
  treats set-but-empty variables as missing (falls back to the default)
  like everyone else.
- A future typed-config need (durations, URLs) gets added to the shared
  package, not re-implemented per service.
