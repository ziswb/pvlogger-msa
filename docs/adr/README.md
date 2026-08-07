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

## Candidate ADRs (implicit decisions worth writing down)

- **All Modbus data flows through the data accessor REST API** — and FusionSolar
  deliberately doesn't (writes `fsdata` directly, no Flyway). Confirm as a rule +
  documented exception, or schedule consolidation.
- **Hand-rolled Modbus TCP** in `pvlogger-shared-modbus` instead of a library
  (the vendored `simonvetter_modbus` / `go-modbus` checkouts were evaluated and
  not adopted — why?).
- **Mock server scope**: dev-only or also deployed?
- **Tilt vs Helm** as the local dev loop (Tiltfile currently references dead paths).
