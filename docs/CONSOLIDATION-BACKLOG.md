# Consolidation Backlog — pvlogger-msa

Verified divergences and defects from a full repo review (2026-08-06), with file references. When an item is resolved
with a "from now on we always do X" outcome, record it as an ADR.

Legend: 🔴 likely defect · 🟡 inconsistency worth consolidating · ⚪ cosmetic.

## 🔴 Likely defects (verify, then fix)

1. ~~**masterdata-server prod config DB-name mismatch**~~

   **Resolved 2026-08-07** in two steps. (a) `dbName` corrected to `pvmasterdata` in
   `application-prod.conf` — the wrong value was dormant (only first-boot self-provisioning reads it, and `fsdata`
   happened to exist) but broke fresh-server bootstrap. (b) The mismatch class eliminated: `DatabaseConfig`
   derives `dbServerCanonicalUrl` from `dbServerUrl + dbName + dbUrlParams`
   (**ADR-0008**); the PostgreSQL profiles of masterdata-server and data-accessor state the name once, only the H2
   configs still override the canonical URL explicitly. The data-accessor `TestBase` dbName copy-paste (`pvmasterdata` →
   `pvdata`) fixed too; deprecated sunrise-sunset deliberately untouched (item 28). Reaches prod with the next image
   rebuild of both services.
2. ~~**`sumUp` adds the same value N times**~~

   **Resolved 2026-08-08.**
   `data-accessor-service.go` looped over `deviceIds` but called
   `retrieveEnergyTodaySoFar(md)` without a device parameter (latent: single virtual meter → loop ran once). Root cause:
   the backing endpoint
   `read-kw-today-so-far/{type}/{registerAddress}` is device-agnostic — no deviceId in the route and the SQL never
   filters on `logic_device_id` — so one call already covers all devices. The loop (and `sumUp`,
   `computeImported/ExportedEnergyToday` with their duplicate masterdata fetches) was removed rather than parameterized;
   regression test added (the module's first).
3. ~~**SQL logger in production code**~~

   **Resolved 2026-08-09**: the
   `addLogger(StdOutSqlLogger)` call in `RegisterReader.kt` (data accessor) was removed without replacement. It dumped
   SQL to stdout on every production call, bypassing logback — and never captured the raw-JDBC daily-delta query it sat
   next to anyway (`StdOutSqlLogger` only sees Exposed-pipeline statements). Log-output-only change, no functional
   impact, no ADR needed.
4. ~~**Frontend dev environment points nowhere**~~

   **Resolved 2026-08-09**:
   the hard-coded `localhost:8090` in `environment.ts` was the port of a personal, never-committed IntelliJ run config —
   dev only ever worked on that one machine. Now `ng serve` proxies `/v1` → data accessor (:8083)
   and `/livedata` (ws) → frontend Ktor (:8085) via `proxy.conf.json`, mirroring the Traefik ingress routing, and
   `environment.ts` uses the same same-origin `location.host` URLs as prod. Don't reintroduce absolute backend URLs in
   the Angular environments — add routes to the proxy instead.
5. ~~**`logging.Fatal` (= `os.Exit`) inside library code**~~

   **Resolved 2026-08-09** (**ADR-0010**): all ~60 library-code Fatals converted to wrapped error returns —
   pvlogger-shared-modbus (masterdata, persistence, response), the internal layers of all three collectors, and the
   kpi-exporter. `Fatal` remains only in `cmd/` mains, job-dispatch layers, and Must-style startup-config helpers (env
   helpers → item 6). Run-level behavior unchanged (error → exit 1), but defers now run on error paths (metrics push,
   item 11) and the packages are unit-testable.

## 🟡 Duplication & divergence

### Go

6. ~~**Three env-config helpers**~~

   **Resolved 2026-08-09** (**ADR-0011**):
   converged on `pv-logger/config`; the kpi-exporter and fusionsolar copies are deleted. The shared helpers are
   Must-style and now **panic** instead of `logging.Fatal` — testable (the helper tests moved to
   `pv-logger/config/env_test.go`) and defers run. `GetEnvOptional` was absorbed from fusionsolar. Behavior changes only
   on misconfiguration (panic trace/exit 2; fusionsolar-auth now treats set-but-empty as missing, like the rest).
7. ~~**Two persistence clients for the same API**~~

   **Resolved 2026-08-09**:
   the fast collector's private client is now a thin domain wrapper (`StoreRawValues`/`StoreKpiValues`/
   `ReadMeterEnergyToday`) around the shared `pvlogger-shared-modbus/persistence` client; the duplicated HTTP/JSON
   plumbing is gone, as are the shared client's unused
   `// Deprecated:` accessors. Bonus defect fix: `StoreValues` used to ignore the response status (a 500 logged "values
   stored" and returned nil) — non-2xx is now an error for all three collectors; regression test added.
8. ~~**Copied collector code**~~

   **Resolved 2026-08-09** (extract per ADR-0004, not delete): the register-values/device-identifier job code moved to
   `pvlogger-shared-modbus/collector` (device collector's superset version, tests ported along), the byte-identical
   `metrics` package to
   `pvlogger-shared-modbus/metrics`. Full and device collectors now hold only their dispatch layers and full's KPI
   `dataaccessor` package; the fast collector keeps its deliberately hard-coded register path.
9. ~~**REST path constants exist in Kotlin (`pvlogger-shared-rest`) but only as scattered env defaults in Go.**~~

   **Resolved 2026-08-10** (**ADR-0012**):
   a generated source of truth was rejected (over-machinery for two consumers); instead the paths stopped being config.
   They were actually stated in *three* places — the live values came from Ansible defaults via Helm env vars, with the
   Go defaults dead in prod. Now the Go side mirrors the Kotlin route constants as plain constants in
   `pvlogger-shared-modbus/config` (env-overridability removed — the server routes are compiled in, so a path override
   could only break the client), a drift test (`config/paths_drift_test.go`) parses the Kotlin sources and fails the Go
   build on divergence, and the `DA_*_PATH`/`MD_PATH` plumbing is deleted from the infrastructure repo.
   Host/port/protocol stay env-based.
10. ~~**k8s client-go version skew** between `pvlogger-fusionsolar` and
    `pvlogger-fusionsolar-auth` for accessing the same secret.~~

    **Resolved 2026-08-10**: fusionsolar-auth bumped v0.32.0 → v0.35.0 (client-go/api/apimachinery), matching
    fusionsolar. The two
    `internal/k8s/secret.go` files turned out complementary (reader vs writer), not duplicates — no extraction
    warranted. Go has no cross-module version catalog, so nothing *prevents* future skew; re-check when bumping either
    module.
11. ~~**Pushgateway metrics** only in fast+full collectors; device collector, fusionsolar and exporter fail silently.
    Decide: metrics everywhere or an explicit reason why not.~~

    **Resolved 2026-08-12** (**ADR-0013**): explicit reason why not. The failures were never silent at the K8s level —
    every CronJob exits non-zero (ADR-0010) and kube-state-metrics already exposes per-CronJob failure/staleness.
    PrometheusRules + a "Collector health"
    Grafana row (infrastructure repo: `roles/prometheus`,
    `roles/grafana_install`) now surface them; the Pushgateway stays reserved for domain KPI gauges. No collector code
    changed.
12. ~~**Hard-coded daily pause window** (00:00, 00:02–00:04) in
    `pv-fast-data-collector/cmd/collector/main.go` instead of the cron schedule.~~

    **Resolved 2026-08-13** in two halves. (1) **ADR-0014**: the 00:00 skip masked the real defect — `Produced` came
    from the inverter's self-resetting daily register 32114, whose reset lands *after* midnight; daily production now
    derives from the lifetime counter 32106 as a day-delta. (2) **ADR-0015**: a live dongle concurrency test showed the
    SmartDongle is **last-connect-wins** (a new client's connect kills the established connection; nothing is refused),
    so the 00:
    02–00:04 window protects the *device jobs from the fast collector* — it now lives in the CronJob schedules
    (`* 1-23 * * *` + `0-1,5-59 0 * * *` in the Helm chart), `main.go` has no time logic, and monitoring treats the pair
    as one logical job. Follow-ups since resolved by decision: the **full collector's** ADR-0014 violation (`Produced`
    from register 32114's latest value) dissolves under **ADR-0016** — its private KPI path gets deleted, not aligned
    (item 34) — and the two-CronJob fast pair is superseded by the uniform hourly window (item 36). Still optional:
    `TcpHandler`
    reconnect-between-attempts hardening.

### Kotlin

13. ~~**Bootstrap differs per service**~~

    **Resolved 2026-08-14.** The shape is the data accessor's: `install(Koin)` (Ktor plugin, application-scoped — not
    `startKoin{}`'s global context) guarded by the `testing` flag, and `TimeZones.requireConfigured()` in `main()` for
    **DB-touching services only** (it guards the daily-delta SQL against a UTC JVM; the chart's `pvlogger.timezoneEnv`
    helper already sets `TZ` on every pvlogger workload). Applied: frontend switched `startKoin{}` → `install(Koin)`;
    masterdata-server gained the TZ check; the ApplicationStopping/Stopped/StopPreparing shutdown-log subscriptions were
    dropped everywhere as noise. Deliberate exceptions: the mock server stays DI-less (raw TCP tool, nothing to
    inject — its other gaps are items 18/19/21) and deprecated sunrise-sunset (ADR-0005, item 28) was left untouched.
    StatusPages/CallLogging/metrics-registry unification remain items 14/15.
14. ~~**StatusPages** (error handling) present in data accessor + masterdata, absent in sunrise-sunset + frontend.~~

    **Resolved 2026-08-14.** The frontend got a `plugins/StatusPages.kt` mirroring masterdata's shape
    (`BadRequestException` → 400, `Throwable` → 500, plain-text bodies, logged); the accessor's extra
    `DateTimeParseException` handler stays service-specific. Deprecated sunrise-sunset (ADR-0005, item 28) untouched,
    as with item 13. Notes: StatusPages doesn't cover WebSocket sessions, so `/livedata` keeps its own error handling;
    a SPA 404→index.html fallback was deliberately *not* added (single Angular route today — revisit when a second
    client-side route appearzs). Extraction into a shared module remains item 15.
15. ~~**CallLogging** installed in two services, missing in sunrise-sunset (whose
    `Monitoring.kt` is otherwise a byte-copy). Metrics registry constructed four different ways. Extract a tiny shared
    "service bootstrap" into
    `pvlogger-shared-*`.~~

    **Resolved 2026-08-14** (**ADR-0018**): new module **`pvlogger-shared-ktor`** with `configureMonitoring()`
    (CallLogging with `call-id` MDC + CallId(X-Request-Id) + Prometheus registry with the standard binders + `/metrics`
    route — the frontend's richer CallLogging shape won and is now everywhere, making item 16 a pure logback change)
    and `configureStatusPages(extra)` (the accessor passes its `DateTimeParseException` handler through the hook).
    The three per-service `Monitoring.kt`/`StatusPages.kt` copies and the frontend's `Logging.kt` are deleted; the
    frontend also lost its dead extras (unconsumed `NotFoundEvent`, unread global `Metrics.addRegistry`,
    `ApplicationMonitoringPlugin` + Koin lifecycle start/stop logs — same noise item 13 dropped). Mock server (no HTTP
    pipeline) and deprecated sunrise-sunset (ADR-0005) deliberately excluded.
16. ~~**logback.xml**: only masterdata logs `%X{call-id}`; frontend has none at all.~~

    **Resolved 2026-08-14.** Trivial after item 15 put CallId + `call-id` MDC into every HTTP service (ADR-0018):
    masterdata's pattern (with `%X{call-id}`) is the standard — the accessor's file gained the field and the frontend
    got a logback.xml at last (it had been running on logback's DEBUG-root default). The mock server keeps the plain
    pattern (raw TCP, no CallId — the field would always render empty); deprecated sunrise-sunset (ADR-0005) untouched.
17. ~~**HOCON config sets uneven** (prod/dev/test vs prod/dev vs prod-only). (The H2-vs-PostgreSQL half of this item is
    obsolete since 2026-08-07: H2 is removed, all profiles are PostgreSQL — ADR-0009.)~~

    **Resolved 2026-08-14.** Standard: `application-prod.conf` always, `application-dev.conf` where a local run
    differs from prod, **no `application-test.conf`** — no test suite ever loaded one (JVM tests build config
    programmatically via `MapApplicationConfig`; Testcontainers ports are dynamic, ADR-0009). The three test confs
    (accessor, masterdata, frontend's dead copy in `src/test/resources`) are deleted; they were near-duplicates of dev
    (only password strictness and pool size differed). **Operator action**: the personal IntelliJ run configs
    `DataAccessor` and `MasterDataServer` must switch `-config=application-test.conf` → `-config=application-dev.conf`
    (the `DB_PASSWORD` env they set keeps working — dev reads `${?DB_PASSWORD}`). Mock server stays prod-only (nothing
    environment-specific in its conf); deprecated sunrise-sunset (ADR-0005) untouched.
18. ~~**Duplicated code**: `MasterDataPaths` object copied between masterdata-server and mock server (belongs in
    `pvlogger-shared-rest`); `PvHttpClient` duplicated between mock server and frontend.~~

    **Resolved 2026-08-14.** Both copies were byte-identical. `MasterDataPaths` → `pvlogger-shared-rest` (next to
    `DataAccessorPaths`; the Go drift test's hard-coded Kotlin path was updated with it — ADR-0012 still holds).
    `PvHttpClient` → `pvlogger-shared-ktor` (its `var client` became `val` — nothing ever reassigned it). Mock server
    now depends on both shared modules and lost four client/serialization deps it no longer references directly;
    the frontend lost three.
19. ~~**Mock server hard-codes the cluster DNS name** of the masterdata server (`MasterDataCache.kt`) instead of reading
    config like every other client.~~

    **Resolved 2026-08-15.** New `pvlogger.masterdata { host, port }` section in the mock server's
    `application-prod.conf` (defaults `localhost:8081`), overridable via `MD_HOST`/`MD_PORT` — the same env names the
    Go masterdata clients use (`pvlogger-shared-modbus/config/master-data-config.go`), so manifests stay uniform per
    target service. Read at startup in `Application.module` and set on `MasterDataCache` via `configure(host, port)`
    (no Koin introduced — the mock server has no DI and is a local test tool). Consequence: an in-cluster (Tilt) run
    now needs `MD_HOST=pvlogger-masterdata-server-service.pvlogger.svc.cluster.local` in its manifest — fold into the
    Tilt repair (item 23).
20. ~~**Business logic under `rest/`** in the data accessor (`HouseKpiController`,
    `RegisterReader` run transactions directly; raw JDBC/`exec()` escape hatches in two places for the same "daily
    delta" pattern) while other services use the `rest/ → service/ → database/access/` layering CLAUDE.md describes.~~

    **Resolved 2026-08-15.** The `exec()` duplication had already collapsed to a single parameterized
    `computeEnergyDelta(column)` in `RawKpiRegisterValuesDao` during the ADR-0014 work; only the layering remained.
    `HouseKpiController` is now routes-only — its queries and `KpiValuesDto` rounding moved into `HousePowerKpiDao`
    (same table as `store()`). `RegisterReader` was a pure DAO and moved to `database/access/` as `RegisterValueDao`.
    No `service/` layer introduced: like masterdata-server, it exists only where there is real business logic, and
    date-window computation + rounding are thin. Behavior-neutral (query order/bounds preserved, incl. `todaysKpis()`
    DESC vs `kpisOf()` ASC).
21. ~~**Mock server build file** is the only one pinning a plugin outside
    `libs.versions.toml` (owasp dependencycheck) and lacks the ShadowJar workaround the other services carry (see
    TROUBLESHOOTING.md).~~

    **Resolved 2026-08-15.** The owasp-dependencycheck plugin was **removed**, not centralized: it was unconfigured,
    referenced nowhere else, and applied only to the least security-relevant service — a leftover experiment (its
    redundant `buildscript { classpath }` block went with it; reintroduce via the catalog if ever wanted). The
    ShadowJar workaround (`mergeServiceFiles()` + `duplicatesStrategy = INCLUDE`) was added to the mock server **and
    the frontend**, which had the same gap (fat JAR in Docker, no Flyway so no visible symptom yet); TROUBLESHOOTING.md's
    affected-services list updated. Mock server 3.2.2, frontend 3.9.1.

## ⚪ Hygiene

22. **Test coverage very uneven**: `pv-full-data-collector` and the shared Kotlin `-rest`/`-database` libs have zero
    tests; frontend has one.
23. **Tiltfile rot**: references non-existent `pvlogger-infrastructure/` paths, no
    `docker_build` steps. Decision (2026-08-07, ex open question 8): **fix, don't remove** — Tilt is the intended dev
    loop for larger changes. Low priority; pairs with the mock-server refresh (item 30).
24. Committed IDE files (`.ipr`, `.iws`, `.iml`), `.DS_Store`; simulator output files (`summary-2024.*`) committed at
    module root.
25. `pvlogger-frontend/src/web/README.md` is stock Angular-CLI boilerplate citing the wrong version.
26. `pvlogger-database/migration/irrelevant/` — delete or archive; the PG18 migration guide is a finished runbook and
    could move to a `docs/history/`.
27. ~~**Site coordinates hard-coded in sunrise-sunset**~~

    **Superseded 2026-08-07**: the service turned out to be deprecated (ADR-0005), so the hard-coded coordinates (the
    operator's home — personal location data in repo and image) disappear with the module. Folded into item 28.
28. **Retire `pvlogger-sunrise-sunset`** (deprecated per ADR-0005: it gated FusionSolar collection to daylight hours,
    and that gating is no longer wanted). Delete the module (Gradle settings, CLAUDE.md service map, CONTEXT.md rows),
    drop the `sunrisesunset` database, and remove its entry from the chart's db-backup job — the chart/database side is
    infrastructure backlog item 32. Until deleted, the module still ships the operator's home coordinates in its HOCON
    profiles (ex item 27).
29. **Introduce `fsdata` schema migrations** — a known gap since the Kotlin→Go rewrite of fusionsolar (rationale in
    ADR-0002, ex open question 4), not a decision: the DDL exists only in the running DB and old dumps. Two steps:
    (1) **baseline now** — `pg_dump --schema-only` the live `fsdata` and commit it as migration 0001; (2) **wire a
    runner** — recommended: a `migrate` job inside `pvlogger-fusionsolar` itself (job-dispatch pattern; no new service,
    per ADR-0007) using tern (pgx ecosystem) with `go:embed`-ded SQL files, executed manually as a plain K8s Job. The
    operator's original plan — a dedicated manually-activated migration service — would work but fails the ADR-0007 bar;
    keeping migrations next to the sqlc queries they must match is the decisive argument for the in-module job.
30. **Refresh the outdated mock server** (low prio, ex open question 8): unused for a while and out of date. Update it
    so the Tilt environment (item 23)
    works again for larger changes, and consider building e2e tests around it — none exist today. It stays local-only,
    never production (open question 6).
31. ~~🔴 **All 22 data-accessor tests fail on H2**~~

    **Resolved 2026-08-07**
    (ADR-0009): the tests now run against real PostgreSQL 18 via Testcontainers — V6 was PostgreSQL-only syntax and
    unfixable on H2 without a checksum-breaking edit. The move also exposed and fixed a test asserting UTC day-bucketing
    that production (TIMESTAMPTZ, `TZ=Europe/Zurich`) never had. Extended same day: masterdata tests moved to
    Testcontainers too, the H2 dev profiles now point at the local compose PostgreSQL, and H2 is removed entirely — no
    migration needs H2 compatibility anymore. Tests require a running Docker daemon.
32. **Repo-wide `gofmt -w`**: ~13 Go files fail `gofmt -l` (import ordering, struct-field alignment) — pre-existing,
    surfaced when golangci-lint was first run locally (2026-08-09; its gofmt linter is not enabled, so lint stays green
    regardless). One mechanical formatting commit, ideally with nothing else in it; consider enabling the gofmt/gofumpt
    linter in the golangci-lint config afterwards to keep it that way.
33. ~~**Negative live consumption under fast irradiance changes**~~ (observed 2026-08-13)

    **Resolved 2026-08-14** (fast collector 0.12.0, deployed): live KPIs now come from the dongle aggregate block in one
    coherent request — Producing ← 37516, Consuming ← 37500 (measured directly), grid ← 37502 sign-flipped;
    SelfConsuming stays derived (no register). Daytime verification: export sign confirmed (−6.1 kW while exporting),
    refresh
    ~10 s, and `total_active + grid = load` held exactly in every sample. Registers 32080/37113 retired from the fast
    collector; raw-store columns keep their semantics. Original analysis kept below; follow-ups are items 34 (single
    aggregate path) and 35 (masterdata dongle device type). Original: when sun and shadow alternate quickly, the
    frontend chart shows negative consumption. `Consuming = Producing − Exporting +
    Importing` (`KpiLiveData`, pvlogger-shared-modbus), but `Producing`
    (inverter 32080) and the meter power (37113) are sampled sequentially — several Modbus round-trips apart, and the
    inverter refreshes 32080 on a slower internal cycle than the near-real-time meter — so under fast fluctuation the
    two operands describe different moments and the difference can go negative (`SelfConsuming` too). Unrelated to the
    midnight peak (ADR-0014: daily *energy counters*; this is live *power*
    skew). Physically consumption is never negative. **Register research (2026-08-13, verified live):** the
    SmartDongle's own logical device (unit **100** — unit 0 times out, unit 1 returns illegal-data-address)
    serves an aggregate block computed by the dongle itself, readable in **one** 20-register request (= one coherent
    snapshot, no skew):
    - 37498 total input power (U32, W) — DC, all inverters
    - **37500 load power (U32, W) — house consumption, directly**
    - 37502 grid power (I32, W, **positive = import** — sign is inverted vs. meter register 37113 where positive =
      export)
    - 37504 battery power (I32, W)
    - 37516 total active power (I32, W) — AC sum over all cascaded inverters (also removes the current two-inverter
      double-read)
      Night verification: load 624 W = grid import 624 W, production 0 — exactly consistent. Register names/types per
      the `wlcrs/huawei-solar-lib`
      register table (not in Huawei's inverter interface PDF — it's the *dongle's* register space; absent from our
      masterdata catalog, which has no load register on inverter or meter). Proposed fix: source the live KPIs
      (`Producing`/`Consuming`/grid exchange) from this block instead of combining 32080×2 + 37113; keep ADR-0014's
      energy registers unchanged. Before implementing, verify a daytime snapshot (export-sign of 37502, 37516 under
      production) and the block's refresh cadence. Scope per **ADR-0016** (2026-08-14): implement the live-KPI mapping
      inside the single shared `aggregate(jobId)` path (item 34), not in fast-local code — there is no fast/full
      divergence to document because full's private KPI aggregator is being deleted. The masterdata device type for unit
      100 is item 35 and does not gate this fix (fast reads the block hard-coded).
34. ~~**Single KPI aggregation path** (ADR-0016)~~

    **Resolved 2026-08-14.**
    KPI computation now exists exactly once: `pvlogger-shared-modbus/aggregate`
    (`KpiAggregator.KpisFor(jobId)`) reads the run's rows back from the data accessor **per jobId** (hard invariant —
    never "newest value per register"), derives + stores the KPIs and pushes the metrics. Live mapping per item 33
    (Producing ← 37516, Consuming ← 37500, grid ← 37502 sign-flipped) from the **generic register store**: fast now
    POSTs the dongle block there (reserved filler 37506–37515 filtered out) in addition to the raw KPI row, then invokes
    the aggregator; full invokes it after its sweep. Deleted, not aligned: fast's local KPI computation/fallback, full's
    `dataaccessor` package + `kpi-aggregator.go` (the 32080/37113 by-jobId route), the masterdata single-register
    helpers, and `KpiLiveData`'s Consuming formula fallback (`ConsumingPower` is now required). The hard-coded KPI
    register set moved to `pvlogger-shared-modbus/registers`
    (wire-complete definitions; a Kotlin round-trip test pins the exact Go wire JSON). Daily figures keep coming from
    `meter-energy-today`; the
    "no lifetime baseline → daily register 32114" fallback moved server-side into that endpoint (newest row's
    `inverter_daily_yield`; null only when the store is empty). Accessor API consolidation riding along:
    `read-latest-register-value` moved under `/v1` like every other route, and the orphaned `read-kw-today-so-far`
    endpoint (only ever used by full's deleted private path — a second, competing day-delta) was removed. **Deploy
    ordering: data accessor before the fast collector** (fast relies on the `/v1` route + server-side fallback).
    **Full's aggregation step fails until item 35** puts the unit-100 block into its sweep — do 35 before scheduling
    full (item 36).
35. ~~**Masterdata dongle device type** (ADR-0016)~~

    **Resolved 2026-08-14**
    (**ADR-0017**). DONGLE added to both device models (Kotlin enum + Go constant; FusionSolar devTypeId 62,
    informational). The 37498×20 block ships as masterdata migration `V2__DongleAggregateBlock.sql` — a Flyway data
    migration, NOT a fourth CSV: the block is project-curated (from
    `wlcrs/huawei-solar-lib`, absent from Huawei's exports) and must stay in lock-step with
    `pvlogger-shared-modbus/registers`; a drift test there parses the migration and fails on divergence (types
    especially — the aggregate's per-type read-back). The undocumented 37506–37515 filler is catalogued (MLD, DYNAMIC)
    on purpose: it keeps 37498..37517 one consecutive run so the collector's merge sweeps the block in a SINGLE Modbus
    request = one coherent snapshot (item 33's invariant); its MLD values land in the accessor's characteristic-curve
    table — accepted noise, hourly. The shared `deviceIdToTypesMap` now includes
    `DONGLE_DEVICE_ID` (default 100) → DONGLE, and the Go masterdata client treats 204 No Content as an empty register
    list (not an error) so the device collector's STATIC/CONFIG sweep no-ops for the dongle instead of failing. Verified
    live in prod masterdata: addresses 37203–37600 were free, no unique-index collision. **Rollout addendum (2026-08-14,
    masterdata 3.3.1):** the first rollout (3.3.0) failed on a pkey collision — prod's id sequence was never advanced
    past the data (the table was populated by a data copy during the PG18 migration), and
    `on conflict (address)` does not arbitrate pkey conflicts. The failed migration rolled back cleanly incl. its
    history row (transactional DDL), so V2 was edited in place — legal, it never applied anywhere durable — and now runs
    `setval(max(id)+1)` before inserting; verified against a simulated prod state on PG18 (desynced sequence → success,
    re-run idempotent). Side effect fixed for good: the CSV importer's bare ON CONFLICT DO NOTHING silently swallowed
    the same pkey collisions, so genuinely NEW rows from a re-import would have been dropped. **Deploy
    masterdata-server (applies V2)
    before rebuilding/enabling the full collector**; full's aggregation gate from item 34 is hereby lifted once both are
    deployed. Item 36 remains.
36. ~~**Uniform hourly Modbus window** (ADR-0016)~~

    **Resolved 2026-08-14**
    (infrastructure repo; deployed same day, first 18:02 handover verified live: fast quiet exactly 02–04, full's first
    run succeeded in ~42 s and emitted a KPI row at 18:02:42 through the shared aggregate path — no raw row, values
    dovetailing with fast's neighbors; no failed jobs, no alerts). Fast is a single CronJob again (`0-1,5-59 * * * *`),
    full runs `2 1-23 * * *`
    with `activeDeadlineSeconds: 150` in the job template — an overrun dies
    ~30 s before fast's minute-5 reconnect instead of fighting it (last-connect-wins dongle). The chart-bug prerequisite
  ffffnfrastructure backlog 2) was resolved **by removal**: ADR-0016 abolished the toggle, so both `suspend:` lines and
    the `pvFullDataCollectionMode`/
    `pvCollectorDynamicMode` keys are simply gone. Alert/dashboard cleanup:
    `PvloggerFastCollectorStale` and the dashboard pair-merge retired; fast and full both sit in the generic frequent
    staleness tier, threshold raised 1 h → 2.5 h (full's widest normal gap is its 2-hour hop over hour 0; fast's tight
    freshness signal stays `PvloggerKpiMetricsStale`, 10 min). Full's configmap gained
    `DONGLE_DEVICE_ID`. **Release prep in this repo:** Dockerfile LABELs bumped (accessor 3.9.0, masterdata 3.3.0, fast
    0.13.0, full 0.8.0, device 3.7.0 — the backlog 34/35 changes) matching the chart tags. Deploy: `build.sh` the five
    images → run the pvlogger playbook (applies masterdata V2, new schedules, unsuspended full in one rollout) → re-run
    the prometheus/grafana roles for the alert/dashboard halves. Verify a full-hour handover afterwards like ADR-0015's
    was verified.
