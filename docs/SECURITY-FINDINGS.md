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
> accident — see item 45.

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

42. **Unauthenticated mutating write endpoints on the data accessor** — *found
    2026-08-17.* 🔴

    `pvlogger-modbus-data-accessor/.../rest/StoreModbusValueController.kt:21,27`,
    `.../rest/StoreKpiValueController.kt:16` and
    `.../rest/RawKpiRegisterValuesController.kt:18` accept measurement writes with no
    auth. Any workload that can reach the pod can inject arbitrary register and KPI
    rows.

    This is the sharper edge of the same missing-auth root cause as item 33, and it
    ranks **above** it: injected rows poison the lifetime-counter arithmetic in
    `RawKpiRegisterValuesDao.computeEnergyDelta` (**ADR-0014**), so a single bogus
    counter value corrupts a day's derived energy KPIs and silently propagates into the
    exported JSON and the dashboard. Unlike item 33 this is a data-integrity risk, not
    just wasted work. Mitigated today only by NetworkPolicies. Fix by restricting the
    write paths to the collector service accounts, or by accepting and recording the
    trust boundary (item 45).

    *Corrected 2026-10-20 — reachable from the whole LAN and the tailnet, not only
    in-cluster.* The "mitigated by NetworkPolicies" above is wrong. The Traefik
    IngressRoute (`PathPrefix(/v1)`) and the Tailscale Ingress (`path: /v1`) forward
    every `/v1` path to the data accessor, and the NetworkPolicies admit both ingress
    namespaces on purpose. All four write routes are under `/v1`. Probed with GET on
    `<pvlogger-host>` and `pvlogger.<tailnet>.ts.net`: `store-kpi-values`,
    `store-modbus-values`, `store-raw-kpi-register-values` answer 405 (route present,
    so a POST would land), an unknown path 404. Given the flat LAN (infrastructure
    finding 54), a guest or IoT device can write KPI rows. **Fix: narrow both ingress
    rules to `/v1/daydata`**, the only data-accessor route the SPA calls — an
    infrastructure chart change, handed over 2026-10-20. The rule behind it is
    **ADR-0020** (proposed): in-cluster calls trusted, the ingress exposes an
    allowlist. Close this item once the narrowed rules are deployed and the probe
    answers 404.

### 🟡 Hardening

33. **Unprotected CSV import endpoint.** *Severity revised down 2026-08-17.*

    `pvlogger-masterdata-server/.../rest/MasterDataController.kt:71-83` exposes
    `put("/$IMPORT_MASTER_DATA_PATH")` (registered unguarded at
    `configuration/Routing.kt:11`), which runs `importInverterRegister()`,
    `importDeviceIdentifiers()` and `importMeterRegister()`.

    The original write-up called this "anyone can modify device register definitions".
    Re-verification shows that overstated it: the importer reads **three fixed CSV paths
    from config** (`application-prod.conf:31-35`), never from the request, so there is no
    attacker-controlled payload or file path; and writes go through
    `MasterDataDao.insertBatch(... ignore = true ...)` (`MasterDataDao.kt:22`) —
    insert-ignore against the unique address index, hence idempotent with no delete or
    overwrite. Residual risk is an unauthenticated repeated CSV parse + batch insert,
    i.e. cost and lock churn, not tampering. Note also that **ADR-0017** demoted this
    importer to "Huawei's own exports only" and nothing in the repo calls the route —
    so deleting it is a legitimate fix, and probably the cheapest one.

18. **`/metrics` unprotected.** *Still present, now centralized.*

    `pvlogger-shared-ktor/.../Monitoring.kt:24,51-55` serves `/metrics` with no auth or
    IP restriction, for data-accessor, masterdata-server and frontend (each
    `Application.kt:32`). The duplicate copy in the deprecated sunrise-sunset module is
    gone — the module was deleted 2026-08-27 (backlog item 28).

    Low severity on inspection: only JVM, process and HTTP meters are registered
    (`Monitoring.kt:41-49`) — no PV or business data is exposed — and no Ingress routes
    `/metrics`, so reachability is cluster-internal. Fix by scoping a NetworkPolicy to
    the monitoring namespace rather than by adding auth.

    *Corrected 2026-10-20:* an Ingress does route it. The **frontend's** `/metrics`
    falls under the `/` rule of both ingresses and answers 200 on `<pvlogger-host>`
    and on the tailnet name (probed). The data accessor's and masterdata server's are not
    exposed (`/metrics` is outside `/v1`; masterdata has no ingress). Severity stays low —
    JVM and HTTP meters only. ADR-0020 (proposed) keeps `/metrics` off the ingress. On
    Traefik that is one rule (`&& !PathPrefix(/metrics)`); the Tailscale Ingress matches
    prefixes only, so there it needs either a separate management port in
    `pvlogger-shared-ktor` — note the chart's liveness/readiness probes use `/metrics` on
    the service port and would move with it — or Tailscale traffic routed through
    Traefik.

27. **No rate limiting on REST endpoints.** *Confirmed unchanged.*

    No `RateLimit` plugin or `ktor-server-rate-limit` dependency anywhere. Normally
    mitigated by Traefik, but there is no application-level protection, and combined
    with items 33 and 42 (unauthenticated writes) there is nothing to bound abuse from
    inside the cluster.

### ⚪ Defence in depth / operational

15. **Database connection pool likely too small.** *Unchanged; reclassified.*

    `maxPoolSize = 3` in `application-prod.conf:22` of data-accessor and
    masterdata-server (a third copy went with sunrise-sunset on 2026-08-27). This is an
    availability/capacity concern rather than a security one — it was mis-filed as P2
    security in the original review. Raise to 10–15 if saturation is ever observed;
    there is no evidence of it today, so this is speculative and low priority.

29. **No shutdown hooks / resource cleanup in JVM services.** *Partially present.*

    The capability exists but is unwired: `pvlogger-shared-database/.../DatabaseFactory.kt:33-35`
    defines `fun close() { datasource?.close() }`, yet it is only called mid-`connect()`
    (line 27) when swapping to the canonical URL. No service subscribes it to shutdown —
    `configuration/Databases.kt:7-10` in both data-accessor and masterdata-server just
    call `connect()`, and no `Application.kt` subscribes to `ApplicationStopping`. The
    only lifecycle subscriptions in the repo used to be log-only ones in sunrise-sunset,
    which went with the module on 2026-08-27 — so there are now none at all.

    Fix: subscribe `DatabaseFactory.close()` to `ApplicationStopping` in the shared
    bootstrap (**ADR-0018**) so all services inherit it. The Go side already does this
    correctly (`pvlogger-fusionsolar/internal/database/connection.go:29-39` plus
    `defer`).

45. **The no-auth posture is undocumented** — *raised 2026-08-17.* ⚪

    Items 33, 42 and the notes below are all one decision wearing five hats, and that
    decision is written down nowhere. Either add auth or record an ADR stating that
    inter-service traffic is trusted, that NetworkPolicies plus the private LAN are the
    enforcement boundary, and that new mutating endpoints therefore need no auth *but do
    need a policy rule*. Without it, the next reviewer re-derives these findings from
    scratch — as this re-verification just did.

    *Update 2026-10-19 — now due.* Infrastructure ADR-0024 (accepted 2026-10-19) plans the first PVLogger interface
    reachable from outside the home: a read-only KPI export endpoint with a token, pulled by the external receiver
    over a shared Tailscale node (where it lives is open question 12). From then on "no auth anywhere" is no longer
    true, so the ADR should state both rules: in-cluster traffic is trusted, and anything reachable from outside
    needs a token. Also weaker than the posture note implies: infrastructure finding 54 records that the LAN is one
    flat network with no filtering between devices.

    *2026-10-20:* written as **ADR-0020** (proposed): in-cluster calls are trusted, the ingress exposes an allowlist
    (SPA files, `/livedata`, `GET /v1/daydata`), browser-facing reads stay open, anything reachable from outside the
    home needs a token. Writing it surfaced the ingress gap in item 42. Close this item when the ADR is accepted.

59. **The `/livedata` WebSocket accepts frames of any size** — *found 2026-10-20.* ⚪

    `pvlogger-frontend/.../plugins/WebSockets.kt` installs WebSockets with
    `maxFrameSize = Long.MAX_VALUE`. The session reads and discards every incoming frame
    (the client is not expected to send anything), but Ktor buffers each frame whole
    before handing it over, so one client on the LAN or tailnet can send a single huge
    frame and push the frontend out of memory. The SPA sends nothing, so a small limit
    (a few KiB) costs nothing; Ktor then closes the session with "message too big". The
    rest of the WebSocket was reviewed the same day: read-only, client input is never
    used, no data beyond what `/v1/daydata` already serves.

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
28. ~~**Hardcoded config defaults in Go services**~~ — **still fixed**, re-verified
    2026-08-17. `pvlogger-fusionsolar/internal/config/config.go:52,57,58,62,63` and
    `pvlogger-fusionsolar-auth/internal/config/config.go:31-32` use `GetEnvChecked` for
    every credential and required host; the FusionSolar password is conditionally required
    via `GetEnvOptional("FUSIONSOLAR_PASSWORD", useTokenSecret)` (line 53). No credential
    has a hardcoded default — the remaining defaulted values are non-secret (vendor host,
    `postgres` username, ports, K8s secret *names*).
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
