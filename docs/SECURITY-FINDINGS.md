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

## 🔴 Exploitable / data-integrity

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

## 🟡 Hardening

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

40. **`http.Get` without a timeout in the KPI exporter** — *found 2026-08-17.* 🟡

    `pvlogger-kpi-exporter/internal/kpi/client.go:23` calls `http.Get(targetURL)`, which
    uses `http.DefaultClient` and therefore has no deadline. Every other Go HTTP client
    in the repo sets 30s (`pvlogger-shared-modbus/persistence/service.go:23-24`,
    `masterdata/service.go:33-34`, `pvlogger-fusionsolar-auth/.../auth.go:49`), so this
    is an inconsistency as much as a defect: an unresponsive data accessor hangs the
    CronJob indefinitely instead of failing it.

41. **Generated FusionSolar client defaults to an unbounded HTTP client** — *found
    2026-08-17.* 🟡

    `pvlogger-fusionsolar/internal/model/client.gen.go:65` sets
    `client.Client = &http.Client{}` (no timeout) and neither call site overrides it:
    `internal/auth/auth.go:20` uses `model.NewClient(cfg.FsUrl())` and
    `internal/fusionsolar/client.go:21` `model.NewClientWithResponses(...)`, neither
    passing `WithHTTPClient`. Same hang risk as item 40, against a **third-party** API
    where a stalled connection is likelier. The generated file should not be edited; fix
    at the two hand-written call sites.

27. **No rate limiting on REST endpoints.** *Confirmed unchanged.*

    No `RateLimit` plugin or `ktor-server-rate-limit` dependency anywhere. Normally
    mitigated by Traefik, but there is no application-level protection, and combined
    with items 33 and 42 (unauthenticated writes) there is nothing to bound abuse from
    inside the cluster.

## ⚪ Defence in depth / operational

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

## Noted 2026-08-17, not findings

- **CORS protection is config-dependent.** `pvlogger-frontend/.../plugins/Cors.kt:8-9`
  returns early when `isProd`, so the permissive dev settings (`allowCredentials = true`,
  `allowHost("localhost:4200")`) cannot reach production — correct today, since
  `application-prod.conf:12` sets `isProd = true`. Worth knowing that the guard is a
  config flag: a new environment missing it silently re-enables permissive CORS.
- **The `/livedata` WebSocket is unauthenticated**
  (`pvlogger-frontend/.../plugins/WebSockets.kt:54`). Read-only PV telemetry; consistent
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
