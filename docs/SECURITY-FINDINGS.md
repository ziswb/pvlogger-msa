# Security Findings — pvlogger-msa

Application-code findings from the full security review of 2026-03-13, **re-verified
against the working tree on 2026-08-17** (HEAD `7e023286`). Each item cites the files,
so it can be picked up independently. When an item is resolved with a "from now on we
always do X" outcome, record that outcome as an ADR.

The original review covered both repos in one numbered list; the infrastructure half
lives in `infrastructure/docs/SECURITY-FINDINGS.md`. **Item numbers are stable IDs
shared across both documents** — gaps in the sequence here mean the item belongs to the
other repo (or was withdrawn), never that a number is free for reuse. Items 40+ were
added by the 2026-08-17 re-verification.

**Status re-checked 2026-10-19** against HEAD `46a39ca0`: every open item still holds in the code. Open items sit
under **Open** by severity; finished ones under **Resolved**, sorted by number. What to do next is decided in
`CONSOLIDATION-BACKLOG.md` → *Next up* (currently backlog item 40, which also closes item 3 here).

Legend: 🔴 exploitable or data-integrity risk · 🟡 hardening worth doing · ⚪ defence in
depth / operational.

> **Posture note.** This system has **no authentication anywhere** — zero
> `install(Authentication)` / `authenticate {}` usages and no `ktor-server-auth`
> dependency in any module. Every finding below that reads "unauthenticated" is a
> symptom of that one decision, not an independent oversight. The compensating control
> is network isolation: K3S NetworkPolicies (default-deny ingress) plus a private
> <lan-subnet> LAN. Anything that reaches a pod is trusted. That trade-off is
> deliberate for a homelab, but it should be a recorded decision rather than an
> accident — see item 45, resolved 2026-10-20 by **ADR-0020** (in-cluster calls
> trusted, the ingress exposes an allowlist, outside access needs a token).

## Open

### 🔴 Exploitable / data-integrity

3. **Database SSL disabled — plaintext credentials on the wire.** *Deferred: needs
   server-side SSL first.*

   `sslmode=disable` is explicit in
   `pvlogger-fusionsolar/internal/database/connection.go:19` and — **found 2026-08-17,
   not in the original review** — in
   `pvlogger-battery-simulator/internal/database/reader.go:20`. SSL is *unset* (the
   PostgreSQL JDBC driver then negotiates no TLS) in every Kotlin service:
   `application-prod.conf:17-19` of data-accessor and masterdata-server set
   `dbUrlParams = "?reWriteBatchedInserts=true"` only, and
   `pvlogger-shared-database/.../DatabaseFactory.kt:61-71` sets no SSL property on the
   `HikariConfig`. (A third instance in `pvlogger-sunrise-sunset` went with the module
   on 2026-08-27, backlog item 28.)

   The DB password itself crosses the wire in the clear on every connect, so a passive
   on-path observer harvests the credential, not just the measurements. Blocked on the
   server: PostgreSQL on the central server has no SSL configured, so the fix is
   generate certs → mount → `ssl = on` → then flip all clients (the JDBC side is a
   one-line `dbUrlParams` change per service thanks to **ADR-0008**). Practical risk is
   held low by the private LAN plus NetworkPolicies. Tackle as a standalone
   infrastructure task; the battery simulator's instance is the least urgent (operator-run
   CLI against the read-only `battery_sim_ro` role).

   *Update 2026-10-19 — unblocked soon.* Infrastructure ADR-0022 (accepted 2026-10-19) moves `pvdata`,
   `pvmasterdata` and `fsdata` to CloudNativePG, which generates server certificates and serves TLS by default.
   The server-side blocker goes away with that move; the client switch is part of **backlog item 40**, which must
   ship before the cutover anyway.

   *Client side done 2026-10-19 (backlog 40; data accessor 3.11.0, masterdata server 3.5.0, fusionsolar 3.4.0).*
   No client hard-codes the TLS mode any more: the JVM services read `sslMode` (`POSTGRESQL_SSLMODE`), fusionsolar
   reads `POSTGRESQL_SSLMODE`, the battery simulator takes `-db-sslmode`. All default to `prefer`, because the
   central server refuses TLS today and `require` would break the release before the move. The finding closes when
   the chart sets `POSTGRESQL_SSLMODE=require` at the cutover. Residual after that: `require` does not verify the
   server certificate (`verify-full` would need CloudNativePG's CA on every client).

### ⚪ Defence in depth / operational

15. **Database connection pool likely too small.** *Unchanged; reclassified.*

    `maxPoolSize = 3` in `application-prod.conf:22` of data-accessor and
    masterdata-server (a third copy went with sunrise-sunset on 2026-08-27). This is an
    availability/capacity concern rather than a security one — it was mis-filed as P2
    security in the original review. Raise to 10–15 if saturation is ever observed;
    there is no evidence of it today, so this is speculative and low priority.

## Noted 2026-08-17, not findings

- **CORS protection is config-dependent.** `pvlogger-frontend/.../plugins/Cors.kt:8-9`
  returns early when `isProd`, so the permissive dev settings (`allowCredentials = true`,
  `allowHost("localhost:4200")`) cannot reach production — correct today, since
  `application-prod.conf:12` sets `isProd = true`. Worth knowing that the guard is a
  config flag: a new environment missing it silently re-enables permissive CORS.
- **The `/livedata` WebSocket is unauthenticated**
  (`pvlogger-frontend/.../plugins/WebSockets.kt:49`). Read-only PV telemetry; consistent
  with the posture note. Listed for completeness.
- **Committed internal hostname in the battery simulator.**
  `pvlogger-battery-simulator/cmd/simulator/main.go:32-35` defaults `-db-host` to
  `<central-host>` and `-db-user` to `battery_sim_ro`. No password is committed
  (it is required from `PGPASSWORD` at `:41-44`). Information disclosure only, and the
  repo is private.

## Resolved

2. ~~**SQL string interpolation in raw queries**~~ — **still fixed**, re-verified
   2026-08-17. `RegisterReader.kt` no longer exists; commit `8a7bc565` moved it to
   `pvlogger-modbus-data-accessor/.../database/access/RegisterValueDao.kt`, which now uses
   the Exposed typed DSL end to end (lines 100-113) — the `PreparedStatement` the original
   fix introduced is gone because there is no raw SQL left to parameterize.
   `RawKpiRegisterValuesDao.kt:28-34` keeps the `EnergyColumn` enum whitelist, and its one
   raw query (lines 60-82) interpolates only `$valueExpr`, sourced exclusively from that
   private enum. Repo-wide: the sole raw `exec(` in production Kotlin is that line; all Go
   SQL is sqlc-generated with `$n` placeholders or otherwise parameterized.
6. ~~**Missing error handling in REST controllers**~~ · 23. ~~**Missing input validation
   on date parameters**~~ · 37. ~~**Exception handling leaking HTTP response bodies**~~ —
   **still fixed**, re-verified 2026-08-17. All three were one fix. StatusPages now lives
   in `pvlogger-shared-ktor/.../StatusPages.kt:16-23` (`BadRequestException` → 400
   "Bad request"; `Throwable` → 500 "Internal server error", cause logged and never
   echoed), installed by data-accessor, masterdata-server **and** frontend
   (`Application.kt:31` in each; the frontend gained it in backlog item 14). The
   data-accessor adds `DateTimeParseException` → 400 "Invalid date format" through the
   `extra` hook (`configuration/StatusPages.kt:14-17`).
7. ~~**HTTP client `LogLevel.ALL` in production**~~ — **still fixed**, re-verified
   2026-08-17. `pvlogger-shared-ktor/.../PvHttpClient.kt:14` sets `LogLevel.INFO`; the
   per-service copies were deduplicated into the shared library in commit `81b3aa5f`
   (**ADR-0018**). The last per-service `HttpClient(` was in sunrise-sunset and went with
   that module on 2026-08-27, so the shared client is now the only one. No
   `LogLevel.ALL`/`BODY`/`HEADERS` anywhere.
8. ~~**Trace-level root logger in production**~~ — **still fixed**, re-verified
   2026-08-17. All `logback.xml` files have `<root level="INFO">` (four since
   sunrise-sunset was deleted on 2026-08-27) and
   `pvlogger-shared-ktor/.../Monitoring.kt:29` sets `CallLogging { level = Level.INFO }`.
9. ~~**HTTP client without timeout (fusionsolar-auth)**~~ — **still fixed**, re-verified
   2026-08-17: `pvlogger-fusionsolar-auth/internal/auth/auth.go:49` sets
   `Timeout: 30 * time.Second`. Note this fix was *not* applied repo-wide — see items 40
   and 41.
17. ~~**Go services use `panic()` instead of `Fatal()`**~~ — **resolved by design**,
    re-verified 2026-08-17. Both cited files
    (`pvlogger-fusionsolar/internal/config/config.go`,
    `pvlogger-fusionsolar-auth/internal/config/config.go`) now contain no `panic(`; they
    delegate to the shared helpers. The remaining panics are confined to Must-style
    startup-config helpers in `pv-logger/config/env.go:25,35,43,51,68` (plus timezone
    setup in `pvlogger-kpi-exporter/internal/datetime/format.go:19,23`), which
    `env.go:10-12` documents as deliberate: startup configuration only, per **ADR-0010**
    and **ADR-0011**. Nothing to do.
18. ~~**`/metrics` unprotected**~~ — **fixed 2026-10-23** on the infrastructure side, under **ADR-0020** (`/metrics`
    stays off the ingress). Served by `pvlogger-shared-ktor/.../Monitoring.kt` for data accessor, masterdata server
    and frontend with no auth (JVM, process and HTTP meters only, no PV data). Only the frontend's was reachable from
    outside the cluster, through the `/` rule of both ingresses. Traefik excludes `/metrics` since 2026-10-20
    (infrastructure `4fa290d`). The Tailscale Ingress matches prefixes only, so it now routes `/metrics` to a Service
    without endpoints (`pvlogger-blackhole`, infrastructure `6882012`, deployed 2026-10-23 14:17). No app change was
    needed: the probes keep using `/metrics` on the service port inside the cluster. Probed 2026-10-23 from the
    tailnet: `/metrics` 502 on `pvlogger.<tailnet>.ts.net`, `/` and `/v1/daydata` 200; `/metrics` 404 on
    `<pvlogger-host>`.
27. ~~**No rate limiting on REST endpoints**~~ — **accepted 2026-10-20** under **ADR-0020**.
    Still no `RateLimit` plugin anywhere, but the reason it mattered — unauthenticated
    writes and the masterdata import reachable from the LAN (items 42, 33) — is gone: the
    ingress now passes only the SPA's files, `/livedata` and `GET /v1/daydata`. Residual: a
    LAN or tailnet client can call `/v1/daydata` in a loop, one SQL query each; the
    `/livedata` fan-out is a shared flow, so more clients do not mean more data-accessor
    load (ADR-0019). Revisit if the ADR-0024 export endpoint needs throttling.
28. ~~**Hardcoded config defaults in Go services**~~ — **still fixed**, re-verified
    2026-08-17. `pvlogger-fusionsolar/internal/config/config.go:52,57,58,62,63` and
    `pvlogger-fusionsolar-auth/internal/config/config.go:31-32` use `GetEnvChecked` for
    every credential and required host; the FusionSolar password is conditionally required
    via `GetEnvOptional("FUSIONSOLAR_PASSWORD", useTokenSecret)` (line 53). No credential
    has a hardcoded default — the remaining defaulted values are non-secret (vendor host,
    `postgres` username, ports, K8s secret *names*).
29. ~~**No shutdown hooks / resource cleanup in JVM services**~~ — **fixed 2026-10-20**
    (data accessor 3.12.1, masterdata server 3.5.1). `DatabaseFactory.close()` existed but
    nothing called it on shutdown, so each stop left the Hikari pool to the JVM exit. Now
    `DatabaseFactory` is `AutoCloseable`, and `closeOnShutdown(...)` in
    `pvlogger-shared-ktor/.../Lifecycle.kt` closes it on `ApplicationStopped` (after
    in-flight requests, unlike the `ApplicationStopping` first suggested here); both
    services' `configureDatabases()` register it. The frontend has no database.
    `LifecycleTest` covers the helper; the data accessor's `ApplicationTest` counts
    connections in `pg_stat_activity` after the application stops — 0 with the hook, 2
    left open without it.
33. ~~**Unprotected CSV import endpoint**~~ — **accepted 2026-10-20** under **ADR-0020**
    rule 1. The import route stays unauthenticated, but the masterdata server has no
    ingress route (checked in the chart the same day), so only pods in the `pvlogger`
    namespace can reach it. It reads three fixed CSV paths and inserts idempotently, and
    ADR-0017 keeps it as the path for Huawei's own exports, so it stays.
40. ~~**`http.Get` without a timeout in the KPI exporter**~~ — **fixed 2026-10-20** (KPI
    exporter 3.4.2). `pvlogger-kpi-exporter/internal/kpi/client.go` used `http.Get`, i.e.
    `http.DefaultClient` with no deadline, so an unresponsive data accessor would hang the
    CronJob. It now uses a package `httpClient` with a 30 s timeout, like the other Go
    clients. `client_test.go` runs `FetchKpis` against a server that never answers and
    expects it to give up.
41. ~~**Generated FusionSolar client defaults to an unbounded HTTP client**~~ — **fixed
    2026-10-20** (fusionsolar 3.4.1). The generated `NewClient` falls back to
    `&http.Client{}` without a timeout, and neither hand-written call site overrode it. The
    generated file stays untouched: a hand-written `internal/model/http_client.go` adds
    `WithTimeout()` (30 s, `model.RequestTimeout`), passed by both `auth.Authenticate` and
    `FusionSolarAccess.newClient`. A test per call site runs it against a server that never
    answers; without the option both tests hang.
42. ~~**Unauthenticated mutating write endpoints on the data accessor**~~ — **fixed
    2026-10-20** by narrowing the ingress (infrastructure `4fa290d`, deployed 23:10), under
    **ADR-0020**. Both ingresses forwarded every `/v1` path, so the collectors' write
    endpoints were reachable from the whole flat LAN and the tailnet, not just in-cluster as
    first written. Traefik now matches `PathPrefix(/v1/daydata) && Method(GET)`, the
    Tailscale Ingress `/v1/daydata`. Checked with GET on both hosts afterwards: daydata 200,
    `store-kpi-values` and `read-latest-kpi` 404, `/` 200, `/livedata` upgrade 101. The
    other `/v1` reads had only in-cluster callers (data accessor log), and Home Assistant
    uses no PVLogger URL. The write routes themselves stay unauthenticated, as ADR-0020
    rule 1 allows.
45. ~~**The no-auth posture is undocumented**~~ — **resolved 2026-10-20** by **ADR-0020**
    (accepted the same day): in-cluster calls are trusted (default-deny NetworkPolicy), the
    ingress exposes an allowlist (SPA files, `/livedata`, `GET /v1/daydata`), browser-facing
    reads stay unauthenticated, and anything reachable from outside the home needs a token
    (the ADR-0024 export endpoint, open question 12). Writing it surfaced that the ingress
    did not follow that rule — fixed the same day (item 42).
59. ~~**The `/livedata` WebSocket accepts frames of any size**~~ — **fixed 2026-10-20**
    (frontend 3.11.2). *Found the same day.* `plugins/WebSockets.kt` set
    `maxFrameSize = Long.MAX_VALUE`; Ktor buffers a frame whole, so one LAN or tailnet client
    could push the frontend out of memory with a single frame. The SPA sends nothing, so the
    limit is now `MAX_INCOMING_FRAME_BYTES` = 4 KiB, and a bigger frame closes the session
    with 1009. `WebSocketsLimitTest` covers both directions: an oversized incoming frame is
    refused (the test fails with the old value), outgoing frames above 4 KiB still go out.
    The rest of the WebSocket was reviewed the same day: read-only, client input is never
    used, nothing beyond what `/v1/daydata` serves.

Also fixed before the 2026-03-13 review and still holding: **CORS lockdown** (commits
`a581720`, `2cbd3f5`, `fc3dbd2` — see the note above) and **`development = false` in all
`application-prod.conf` files** (commit `d7d34cf`; re-verified 2026-08-17 at line 2 of
every prod config, with `true` only in dev configs — the one test-config instance went
with sunrise-sunset on 2026-08-27).

## Withdrawn

- The original review recorded a **pre-existing test failure**
  (`ApplicationTest > test so far today energy()` failing with 404 in
  pvlogger-modbus-data-accessor). Re-verified 2026-08-17: **no test of that name exists**
  — the data-accessor test sources were reorganized by the Testcontainers migration
  (**ADR-0009**) and the item-20 layering refactor into `RawKpiControllerTest`,
  `HouseKpiControllerTest`, `StoreAndReadKpiTest`, `ReadRegisterValueControllerTest`,
  `StatusPagesTest` and a slimmer `ApplicationTest`. Moot, and it was never a security
  finding.
