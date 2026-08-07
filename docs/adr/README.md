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

## Candidate ADRs (implicit decisions worth writing down)

- **All Modbus data flows through the data accessor REST API** — and FusionSolar
  deliberately doesn't (writes `fsdata` directly, no Flyway). Confirm as a rule +
  documented exception, or schedule consolidation.
- **Hand-rolled Modbus TCP** in `pvlogger-shared-modbus` instead of a library
  (the vendored `simonvetter_modbus` / `go-modbus` checkouts were evaluated and
  not adopted — why?).
