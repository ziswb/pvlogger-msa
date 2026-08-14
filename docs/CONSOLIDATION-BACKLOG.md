# Consolidation Backlog — pvlogger-msa

Verified divergences and defects from a full repo review (2026-08-06), with file
references. When an item is resolved with a "from now on we always do X" outcome,
record it as an ADR.

Legend: 🔴 likely defect · 🟡 inconsistency worth consolidating · ⚪ cosmetic.

## 🔴 Likely defects (verify, then fix)

1. ~~**masterdata-server prod config DB-name mismatch**~~ **Resolved
   2026-08-07** in two steps. (a) `dbName` corrected to `pvmasterdata` in
   `application-prod.conf` — the wrong value was dormant (only first-boot
   self-provisioning reads it, and `fsdata` happened to exist) but broke
   fresh-server bootstrap. (b) The mismatch class eliminated: `DatabaseConfig`
   derives `dbServerCanonicalUrl` from `dbServerUrl + dbName + dbUrlParams`
   (**ADR-0008**); the PostgreSQL profiles of masterdata-server and
   data-accessor state the name once, only the H2 configs still override the
   canonical URL explicitly. The data-accessor `TestBase` dbName copy-paste
   (`pvmasterdata` → `pvdata`) fixed too; deprecated sunrise-sunset deliberately
   untouched (item 28). Reaches prod with the next image rebuild of both
   services.
2. ~~**`sumUp` adds the same value N times**~~ **Resolved 2026-08-08.**
   `data-accessor-service.go` looped over `deviceIds` but called
   `retrieveEnergyTodaySoFar(md)` without a device parameter (latent: single
   virtual meter → loop ran once). Root cause: the backing endpoint
   `read-kw-today-so-far/{type}/{registerAddress}` is device-agnostic — no
   deviceId in the route and the SQL never filters on `logic_device_id` — so
   one call already covers all devices. The loop (and `sumUp`,
   `computeImported/ExportedEnergyToday` with their duplicate masterdata
   fetches) was removed rather than parameterized; regression test added
   (the module's first).
3. ~~**SQL logger in production code**~~ **Resolved 2026-08-09**: the
   `addLogger(StdOutSqlLogger)` call in `RegisterReader.kt` (data accessor) was
   removed without replacement. It dumped SQL to stdout on every production
   call, bypassing logback — and never captured the raw-JDBC daily-delta query
   it sat next to anyway (`StdOutSqlLogger` only sees Exposed-pipeline
   statements). Log-output-only change, no functional impact, no ADR needed.
4. ~~**Frontend dev environment points nowhere**~~ **Resolved 2026-08-09**:
   the hard-coded `localhost:8090` in `environment.ts` was the port of a
   personal, never-committed IntelliJ run config — dev only ever worked on
   that one machine. Now `ng serve` proxies `/v1` → data accessor (:8083)
   and `/livedata` (ws) → frontend Ktor (:8085) via `proxy.conf.json`,
   mirroring the Traefik ingress routing, and `environment.ts` uses the same
   same-origin `location.host` URLs as prod. Don't reintroduce absolute
   backend URLs in the Angular environments — add routes to the proxy instead.
5. ~~**`logging.Fatal` (= `os.Exit`) inside library code**~~ **Resolved
   2026-08-09** (**ADR-0010**): all ~60 library-code Fatals converted to
   wrapped error returns — pvlogger-shared-modbus (masterdata, persistence,
   response), the internal layers of all three collectors, and the
   kpi-exporter. `Fatal` remains only in `cmd/` mains, job-dispatch layers,
   and Must-style startup-config helpers (env helpers → item 6). Run-level
   behavior unchanged (error → exit 1), but defers now run on error paths
   (metrics push, item 11) and the packages are unit-testable.

## 🟡 Duplication & divergence

### Go

6. ~~**Three env-config helpers**~~ **Resolved 2026-08-09** (**ADR-0011**):
   converged on `pv-logger/config`; the kpi-exporter and fusionsolar copies
   are deleted. The shared helpers are Must-style and now **panic** instead
   of `logging.Fatal` — testable (the helper tests moved to
   `pv-logger/config/env_test.go`) and defers run. `GetEnvOptional` was
   absorbed from fusionsolar. Behavior changes only on misconfiguration
   (panic trace/exit 2; fusionsolar-auth now treats set-but-empty as
   missing, like the rest).
7. ~~**Two persistence clients for the same API**~~ **Resolved 2026-08-09**:
   the fast collector's private client is now a thin domain wrapper
   (`StoreRawValues`/`StoreKpiValues`/`ReadMeterEnergyToday`) around the
   shared `pvlogger-shared-modbus/persistence` client; the duplicated
   HTTP/JSON plumbing is gone, as are the shared client's unused
   `// Deprecated:` accessors. Bonus defect fix: `StoreValues` used to
   ignore the response status (a 500 logged "values stored" and returned
   nil) — non-2xx is now an error for all three collectors; regression
   test added.
8. ~~**Copied collector code**~~ **Resolved 2026-08-09** (extract per
   ADR-0004, not delete): the register-values/device-identifier job code
   moved to `pvlogger-shared-modbus/collector` (device collector's superset
   version, tests ported along), the byte-identical `metrics` package to
   `pvlogger-shared-modbus/metrics`. Full and device collectors now hold only
   their dispatch layers and full's KPI `dataaccessor` package; the fast
   collector keeps its deliberately hard-coded register path.
9. ~~**REST path constants exist in Kotlin (`pvlogger-shared-rest`) but only as
   scattered env defaults in Go.**~~ **Resolved 2026-08-10** (**ADR-0012**):
   a generated source of truth was rejected (over-machinery for two
   consumers); instead the paths stopped being config. They were actually
   stated in *three* places — the live values came from Ansible defaults via
   Helm env vars, with the Go defaults dead in prod. Now the Go side mirrors
   the Kotlin route constants as plain constants in
   `pvlogger-shared-modbus/config` (env-overridability removed — the server
   routes are compiled in, so a path override could only break the client),
   a drift test (`config/paths_drift_test.go`) parses the Kotlin sources and
   fails the Go build on divergence, and the `DA_*_PATH`/`MD_PATH` plumbing
   is deleted from the infrastructure repo. Host/port/protocol stay env-based.
10. ~~**k8s client-go version skew** between `pvlogger-fusionsolar` and
    `pvlogger-fusionsolar-auth` for accessing the same secret.~~ **Resolved
    2026-08-10**: fusionsolar-auth bumped v0.32.0 → v0.35.0
    (client-go/api/apimachinery), matching fusionsolar. The two
    `internal/k8s/secret.go` files turned out complementary (reader vs
    writer), not duplicates — no extraction warranted. Go has no
    cross-module version catalog, so nothing *prevents* future skew;
    re-check when bumping either module.
11. ~~**Pushgateway metrics** only in fast+full collectors; device collector,
    fusionsolar and exporter fail silently. Decide: metrics everywhere or an
    explicit reason why not.~~ **Resolved 2026-08-12** (**ADR-0013**): explicit
    reason why not. The failures were never silent at the K8s level — every
    CronJob exits non-zero (ADR-0010) and kube-state-metrics already exposes
    per-CronJob failure/staleness. PrometheusRules + a "Collector health"
    Grafana row (infrastructure repo: `roles/prometheus`,
    `roles/grafana_install`) now surface them; the Pushgateway stays reserved
    for domain KPI gauges. No collector code changed.
12. ~~**Hard-coded daily pause window** (00:00, 00:02–00:04) in
    `pv-fast-data-collector/cmd/collector/main.go` instead of the cron
    schedule.~~ **Resolved 2026-08-13** in two halves. (1) **ADR-0014**: the
    00:00 skip masked the real defect — `Produced` came from the inverter's
    self-resetting daily register 32114, whose reset lands *after* midnight;
    daily production now derives from the lifetime counter 32106 as a
    day-delta. (2) **ADR-0015**: a live dongle concurrency test showed the
    SmartDongle is **last-connect-wins** (a new client's connect kills the
    established connection; nothing is refused), so the 00:02–00:04 window
    protects the *device jobs from the fast collector* — it now lives in the
    CronJob schedules (`* 1-23 * * *` + `0-1,5-59 0 * * *` in the Helm
    chart), `main.go` has no time logic, and monitoring treats the pair as
    one logical job. Follow-ups since resolved by decision: the **full
    collector's** ADR-0014 violation (`Produced` from register 32114's latest
    value) dissolves under **ADR-0016** — its private KPI path gets deleted,
    not aligned (item 34) — and the two-CronJob fast pair is superseded by the
    uniform hourly window (item 36). Still optional: `TcpHandler`
    reconnect-between-attempts hardening.

### Kotlin

13. **Bootstrap differs per service**: data accessor uses `install(Koin)` +
    `TimeZones.requireConfigured()`; frontend uses `startKoin{}` without the TZ
    check; mock server has no DI at all. Pick one bootstrap shape.
14. **StatusPages** (error handling) present in data accessor + masterdata,
    absent in sunrise-sunset + frontend.
15. **CallLogging** installed in two services, missing in sunrise-sunset (whose
    `Monitoring.kt` is otherwise a byte-copy). Metrics registry constructed four
    different ways. Extract a tiny shared "service bootstrap" into
    `pvlogger-shared-*`.
16. **logback.xml**: only masterdata logs `%X{call-id}`; frontend has none at all.
17. **HOCON config sets uneven** (prod/dev/test vs prod/dev vs prod-only).
    (The H2-vs-PostgreSQL half of this item is obsolete since 2026-08-07: H2
    is removed, all profiles are PostgreSQL — ADR-0009.)
18. **Duplicated code**: `MasterDataPaths` object copied between masterdata-server
    and mock server (belongs in `pvlogger-shared-rest`); `PvHttpClient` duplicated
    between mock server and frontend.
19. **Mock server hard-codes the cluster DNS name** of the masterdata server
    (`MasterDataCache.kt`) instead of reading config like every other client.
20. **Business logic under `rest/`** in the data accessor (`HouseKpiController`,
    `RegisterReader` run transactions directly; raw JDBC/`exec()` escape hatches
    in two places for the same "daily delta" pattern) while other services use
    the `rest/ → service/ → database/access/` layering CLAUDE.md describes.
21. **Mock server build file** is the only one pinning a plugin outside
    `libs.versions.toml` (owasp dependencycheck) and lacks the ShadowJar
    workaround the other services carry (see TROUBLESHOOTING.md).

## ⚪ Hygiene

22. **Test coverage very uneven**: `pv-full-data-collector` and the shared
    Kotlin `-rest`/`-database` libs have zero tests; frontend has one.
23. **Tiltfile rot**: references non-existent `pvlogger-infrastructure/` paths, no
    `docker_build` steps. Decision (2026-08-07, ex open question 8): **fix, don't
    remove** — Tilt is the intended dev loop for larger changes. Low priority;
    pairs with the mock-server refresh (item 30).
24. Committed IDE files (`.ipr`, `.iws`, `.iml`), `.DS_Store`; simulator output
    files (`summary-2024.*`) committed at module root.
25. `pvlogger-frontend/src/web/README.md` is stock Angular-CLI boilerplate citing
    the wrong version.
26. `pvlogger-database/migration/irrelevant/` — delete or archive; the PG18
    migration guide is a finished runbook and could move to a `docs/history/`.
27. ~~**Site coordinates hard-coded in sunrise-sunset**~~ **Superseded
    2026-08-07**: the service turned out to be deprecated (ADR-0005), so the
    hard-coded coordinates (the operator's home — personal location data in
    repo and image) disappear with the module. Folded into item 28.
28. **Retire `pvlogger-sunrise-sunset`** (deprecated per ADR-0005: it gated
    FusionSolar collection to daylight hours, and that gating is no longer
    wanted). Delete the module (Gradle settings, CLAUDE.md service map,
    CONTEXT.md rows), drop the `sunrisesunset` database, and remove its entry
    from the chart's db-backup job — the chart/database side is infrastructure
    backlog item 32. Until deleted, the module still ships the operator's home
    coordinates in its HOCON profiles (ex item 27).
29. **Introduce `fsdata` schema migrations** — a known gap since the Kotlin→Go
    rewrite of fusionsolar (rationale in ADR-0002, ex open question 4), not a
    decision: the DDL exists only in the running DB and old dumps. Two steps:
    (1) **baseline now** — `pg_dump --schema-only` the live `fsdata` and commit
    it as migration 0001; (2) **wire a runner** — recommended: a `migrate` job
    inside `pvlogger-fusionsolar` itself (job-dispatch pattern; no new service,
    per ADR-0007) using tern (pgx ecosystem) with `go:embed`-ded SQL files,
    executed manually as a plain K8s Job. The operator's original plan — a
    dedicated manually-activated migration service — would work but fails the
    ADR-0007 bar; keeping migrations next to the sqlc queries they must match
    is the decisive argument for the in-module job.
30. **Refresh the outdated mock server** (low prio, ex open question 8): unused
    for a while and out of date. Update it so the Tilt environment (item 23)
    works again for larger changes, and consider building e2e tests around it —
    none exist today. It stays local-only, never production (open question 6).
31. ~~🔴 **All 22 data-accessor tests fail on H2**~~ **Resolved 2026-08-07**
    (ADR-0009): the tests now run against real PostgreSQL 18 via
    Testcontainers — V6 was PostgreSQL-only syntax and unfixable on H2 without
    a checksum-breaking edit. The move also exposed and fixed a test asserting
    UTC day-bucketing that production (TIMESTAMPTZ, `TZ=Europe/Zurich`) never
    had. Extended same day: masterdata tests moved to Testcontainers too, the
    H2 dev profiles now point at the local compose PostgreSQL, and H2 is
    removed entirely — no migration needs H2 compatibility anymore. Tests
    require a running Docker daemon.
32. **Repo-wide `gofmt -w`**: ~13 Go files fail `gofmt -l` (import ordering,
    struct-field alignment) — pre-existing, surfaced when golangci-lint was
    first run locally (2026-08-09; its gofmt linter is not enabled, so lint
    stays green regardless). One mechanical formatting commit, ideally with
    nothing else in it; consider enabling the gofmt/gofumpt linter in the
    golangci-lint config afterwards to keep it that way.
33. ~~**Negative live consumption under fast irradiance changes**~~ (observed
    2026-08-13) **Resolved 2026-08-14** (fast collector 0.12.0, deployed): live
    KPIs now come from the dongle aggregate block in one coherent request —
    Producing ← 37516, Consuming ← 37500 (measured directly), grid ← 37502
    sign-flipped; SelfConsuming stays derived (no register). Daytime
    verification: export sign confirmed (−6.1 kW while exporting), refresh
    ~10 s, and `total_active + grid = load` held exactly in every sample.
    Registers 32080/37113 retired from the fast collector; raw-store columns
    keep their semantics. Original analysis kept below; follow-ups are items
    34 (single aggregate path) and 35 (masterdata dongle device type).
    Original: when sun and shadow alternate quickly, the frontend chart
    shows negative consumption. `Consuming = Producing − Exporting +
    Importing` (`KpiLiveData`, pvlogger-shared-modbus), but `Producing`
    (inverter 32080) and the meter power (37113) are sampled sequentially —
    several Modbus round-trips apart, and the inverter refreshes 32080 on a
    slower internal cycle than the near-real-time meter — so under fast
    fluctuation the two operands describe different moments and the
    difference can go negative (`SelfConsuming` too). Unrelated to the
    midnight peak (ADR-0014: daily *energy counters*; this is live *power*
    skew). Physically consumption is never negative. **Register research
    (2026-08-13, verified live):** the SmartDongle's own logical device
    (unit **100** — unit 0 times out, unit 1 returns illegal-data-address)
    serves an aggregate block computed by the dongle itself, readable in
    **one** 20-register request (= one coherent snapshot, no skew):
    - 37498 total input power (U32, W) — DC, all inverters
    - **37500 load power (U32, W) — house consumption, directly**
    - 37502 grid power (I32, W, **positive = import** — sign is inverted
      vs. meter register 37113 where positive = export)
    - 37504 battery power (I32, W)
    - 37516 total active power (I32, W) — AC sum over all cascaded
      inverters (also removes the current two-inverter double-read)
    Night verification: load 624 W = grid import 624 W, production 0 —
    exactly consistent. Register names/types per the `wlcrs/huawei-solar-lib`
    register table (not in Huawei's inverter interface PDF — it's the
    *dongle's* register space; absent from our masterdata catalog, which has
    no load register on inverter or meter). Proposed fix: source the live
    KPIs (`Producing`/`Consuming`/grid exchange) from this block instead of
    combining 32080×2 + 37113; keep ADR-0014's energy registers unchanged.
    Before implementing, verify a daytime snapshot (export-sign of 37502,
    37516 under production) and the block's refresh cadence. Scope per
    **ADR-0016** (2026-08-14): implement the live-KPI mapping inside the
    single shared `aggregate(jobId)` path (item 34), not in fast-local code —
    there is no fast/full divergence to document because full's private KPI
    aggregator is being deleted. The masterdata device type for unit 100 is
    item 35 and does not gate this fix (fast reads the block hard-coded).
34. **Single KPI aggregation path** (ADR-0016): extract/promote an
    `aggregate(jobId)` component in `pvlogger-shared-modbus` that reads a
    run's raw rows back from the data accessor and derives + stores the KPIs
    (live mapping from the unit-100 block per item 33; daily figures keep
    coming from the accessor's ADR-0014 delta endpoint). Both collectors call
    it in-process after POSTing raw values. Delete fast's private KPI
    computation and full's private aggregator path (incl. its 32080/37113
    by-jobId route). Hard invariant: aggregation is per `jobId` — never
    "newest value per register" across runs (snapshot coherence, see item 33).
35. **Masterdata dongle device type** (ADR-0016): unit 100 is outside
    INVERTER/METER, so the register catalog cannot represent the device that
    serves the primary live-KPI block. Add a DONGLE device type across
    masterdata-server, the shared device model, and the importer; register
    the 37498×20 block (names/types per `wlcrs/huawei-solar-lib`). Then the
    full collector's sweep picks the block up masterdata-driven.
36. **Uniform hourly Modbus window** (ADR-0016): fast back to a single
    CronJob `0-1,5-59 * * * *`; full scheduled `2 1-23 * * *` with
    `activeDeadlineSeconds` inside the window (last-connect-wins dongle — an
    overrun must fail, not fight fast). Prerequisite: the chart bug
    (infrastructure backlog 2). Infra cleanup rides along: retire
    `PvloggerFastCollectorStale` and the dashboard pair-merge, return fast to
    the generic frequent alert tier, add full to it (hourly cadence).
