# Architecture Decision Records

Short records of decisions that shape PVLogger. One file per decision, numbered,
never edited after acceptance — a changed mind gets a new ADR that supersedes the
old one. Half a page max; the point is the *why*. Template: see
`infrastructure/docs/adr/README.md` (same format in both repos).

## Index

- [0001 — Record architecture decisions](0001-record-architecture-decisions.md)
- [0002 — Kotlin/Ktor for services, Go for collectors](0002-kotlin-servers-go-collectors.md)
- [0003 — Image versioning and deployment flow](0003-image-versioning-and-deployment.md)
- [0004 — Three Modbus collectors: fast by default, full as manual alternative, device nightly](0004-three-modbus-collectors-manual-toggle.md)
- [0005 — Retire sunrise-sunset: FusionSolar collection runs around the clock](0005-retire-sunrise-sunset.md)
- [0006 — Defer the battery purchase; keep the simulator for re-evaluation](0006-defer-battery-purchase-keep-simulator.md)
- [0007 — Microservices were chosen for learning; the bar for new services is pragmatic now](0007-microservices-for-learning-pragmatism-now.md)
- [0008 — Database name is stated once; the canonical JDBC URL is derived](0008-derive-canonical-db-url.md)
- [0009 — JVM tests run against real PostgreSQL (Testcontainers); H2 removed](0009-data-accessor-tests-on-real-postgresql.md)
- [0010 — Go library code returns errors; Fatal only in cmd/ and job dispatch](0010-go-libraries-return-errors.md)
- [0011 — Env-config helpers live in pv-logger/config and panic (Must-style)](0011-env-config-helpers-in-pv-logger.md)
- [0012 — REST paths are API contract constants, not deployment config](0012-rest-paths-are-contract-not-config.md)
- [0013 — CronJob health via kube-state-metrics; Pushgateway carries domain KPIs only](0013-cronjob-health-via-kube-state-metrics.md)
- [0014 — Daily energy KPIs derive from lifetime counters, not device daily registers](0014-daily-kpis-from-lifetime-counters.md)
- [0015 — Modbus access windows live in cron schedules, not in code](0015-modbus-windows-live-in-cron-schedules.md)
- [0016 — One KPI path: fast collects per-minute, full sweeps hourly in a uniform window](0016-one-kpi-path-fast-per-minute-full-hourly.md)
- [0017 — Project-curated register definitions enter masterdata via Flyway migration; vendor CSVs stay import-only](0017-project-curated-registers-enter-masterdata-via-migration.md)
- [0018 — Ktor service bootstrap lives in pvlogger-shared-ktor](0018-ktor-bootstrap-in-shared-ktor.md)

## Candidate ADRs (implicit decisions worth writing down)

- **All Modbus data flows through the data accessor REST API** — and FusionSolar
  deliberately doesn't (writes `fsdata` directly, no Flyway). Confirm as a rule +
  documented exception, or schedule consolidation.
- **Hand-rolled Modbus TCP** in `pvlogger-shared-modbus` instead of a library
  (the vendored `simonvetter_modbus` / `go-modbus` checkouts were evaluated and
  not adopted — why?).
