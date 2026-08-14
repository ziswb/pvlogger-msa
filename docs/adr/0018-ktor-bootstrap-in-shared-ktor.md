# 0018 — Ktor service bootstrap lives in pvlogger-shared-ktor

- Status: accepted
- Date: 2026-08-14

## Context

Every Kotlin service hand-rolled its own bootstrap. Consolidation backlog
items 13–15 catalogued the drift: Koin installed two different ways
(`install(Koin)` vs global `startKoin{}`), the timezone guard present in some
`main()`s, StatusPages and CallLogging present in some services, the
Prometheus registry constructed four different ways (including a dead global
`Metrics.addRegistry` and an unconsumed `NotFoundEvent` in the frontend).
Byte-identical `Monitoring.kt` copies had already appeared in two services.

## Decision

HTTP plumbing that every Ktor service needs identically lives once in
**`pvlogger-shared-ktor`**, not per service:

- `configureMonitoring()` — CallLogging (INFO, `call-id` MDC) + CallId
  (X-Request-Id) + a locally constructed Prometheus registry with the standard
  JVM/system binders + the `/metrics` scrape route.
- `configureStatusPages(extra)` — `BadRequestException` → 400, `Throwable` →
  500, plain-text bodies (no exception details leak); service-specific
  handlers go through the `extra` extension hook, not a copied file.

The per-service bootstrap shape (backlog 13) stays in each `Application.kt`:
`install(Koin)` as the Ktor plugin guarded by the `testing` flag, and
`TimeZones.requireConfigured()` in `main()` for DB-touching services.

## Consequences

- A new service copies two calls, not two files; divergence in this layer now
  requires editing shared code visibly.
- `pvlogger-shared-rest` stays a pure wire-contract module (it is parsed by
  the Go drift test, ADR-0012); `pvlogger-shared-database` stays DB-only.
- Every service now emits the `call-id` MDC — backlog item 16 (uneven
  `%X{call-id}` logback patterns) reduces to a logback.xml alignment.
- The mock server (raw TCP, no HTTP pipeline) and the deprecated
  sunrise-sunset (ADR-0005) deliberately don't participate.
