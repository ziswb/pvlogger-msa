# 0002 — Kotlin/Ktor for services, Go for collectors

- Status: accepted (recorded retroactively; decision visible in the code)
- Date: 2026-08-06

## Context

The system has two kinds of workload: long-running HTTP services with database
ownership and REST APIs, and short-lived scheduled batch jobs (Modbus polling,
API pulls, exports) that run as Kubernetes CronJobs on ARM64.

## Decision

- **Long-running HTTP services** are Kotlin with Ktor (not Spring), Koin DI,
  Exposed (not JPA), Kotlinx Serialization, Flyway migrations, HOCON config.
- **Scheduled batch jobs** are Go: single static binary, distroless image, job
  dispatch on a `JOB_NAME` env var, slog JSON logging, config from env.
- Shared code lives in per-language libraries (`pvlogger-shared-*` for Kotlin,
  `pv-logger` + `pvlogger-shared-modbus` for Go).

## Consequences

- CronJob pods start fast and stay tiny (distroless static binaries) — a good fit
  for per-minute schedules on Raspberry Pis; the JVM's startup cost is paid only
  by services that stay up.
- Cross-cutting concerns must be maintained twice (REST path constants exist as a
  Kotlin object but only as scattered env defaults in Go — backlog item).
- ~~⚠️ GAP: the actual historical rationale for the split is not written down; the
  fit described above is inferred. Confirm or correct it.~~ Confirmed 2026-08-07
  (open question 4): fusionsolar was originally a long-running Kotlin/Ktor app
  whose jobs were triggered by a homegrown quartz-based `pvlogger-heartbeat`
  service, and it owned the `fsdata` migrations. With the switch to K8s jobs,
  the JVM's startup cost proved too slow — that drove the rewrite to Go.
  Migrations were knowingly dropped in that rewrite (backlog item 29).
