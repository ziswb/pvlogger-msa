# 0010 — Go library code returns errors; Fatal only in cmd/ and job dispatch

- Status: accepted
- Date: 2026-08-09

## Context

`logging.Fatal` (which calls `os.Exit(1)`) was scattered through Go library
code: the shared `pvlogger-shared-modbus` packages (masterdata, persistence,
response parsing) and the internal service layers of all three collectors and
the kpi-exporter — roughly 60 call sites (consolidation backlog item 5).

`os.Exit` skips deferred functions, so a failing run never executed its
cleanup or metrics-push defers — one reason failures were invisible in
Pushgateway (backlog item 11). Callers could not retry, degrade, or clean up,
and the packages were untestable without killing the test process. The
fusionsolar modules already showed the intended shape: fetch/save functions
return `error`; only `cmd/` and the job-dispatch layer decide to exit.

## Decision

Go code outside `cmd/` **returns errors** (wrapped with
`fmt.Errorf("...: %w", err)`) instead of calling `logging.Fatal`. The
"any error kills the CronJob run" *policy* is unchanged, but it now lives
where policy belongs: `main` (or the job-dispatch switch, which is
quasi-main) logs the error once via `logging.Fatal` and exits non-zero.

Documented exceptions that may still Fatal:

- `cmd/*/main.go` and job-dispatch layers (`internal/jobs/jobs.go` in
  fusionsolar, the job switch in the device collector).
- Must-style startup-config helpers (`pv-logger/config/env.go` and its
  clones — convergence is backlog item 6 — and the kpi-exporter's `TZ`
  check): missing configuration at startup has exactly one sane outcome.

## Consequences

- Run-level behavior is unchanged: a failed collector run still exits
  non-zero and K8s records the CronJob failure.
- Deferred cleanup (connection close, response-body close) now runs on error
  paths, and error paths can be unit-tested with `httptest` servers.
- Decoding a malformed Modbus device-identifier response returns an error
  instead of killing the process (and no longer panics on a short header).
- The near-copied register-value files in the full and device collectors
  (backlog item 8) were converted identically, so a later extract-or-delete
  stays trivial.
