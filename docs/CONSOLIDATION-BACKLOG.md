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
4. **Frontend dev environment points nowhere**: `src/web/.../environment.ts`
   hard-codes `localhost:8090`; the Ktor dev server runs on 8085 and the data
   accessor on 8083.
5. **`logging.Fatal` (= `os.Exit`) inside library code** makes errors
   unrecoverable for callers: `pvlogger-shared-modbus/masterdata/service.go`,
   `pv-full-data-collector/.../data-accessor-service.go`. The fusionsolar modules
   show the better pattern (return `error`, `Fatal` only in `cmd/`).

## 🟡 Duplication & divergence

### Go

6. **Three env-config helpers**: shared `pv-logger/config/env.go`, a
   re-implementation in `pvlogger-kpi-exporter/internal/config/env.go`, and
   `panic()`-based variants in both fusionsolar modules. Converge on `pv-logger`.
7. **Two persistence clients for the same API**:
   `pvlogger-shared-modbus/persistence/service.go` (used by device+full
   collectors, carries `// Deprecated:` markers) vs a private one in
   `pv-fast-data-collector/internal/services/persistence.go`.
8. **Copied collector code**: `pv-full-data-collector`'s
   `read-register-values-{model,service}.go` are near-copies of
   `pv-device-data-collector`'s; the `metrics` package is byte-identical between
   fast and full collectors. Extract to `pvlogger-shared-modbus` — or resolve the
   collector end-state question first (see OPEN-QUESTIONS) and delete instead.
9. **REST path constants exist in Kotlin (`pvlogger-shared-rest`) but only as
   scattered env defaults in Go** — three different
   `internal/config/data-accessor-config.go` files. A shared Go constants package
   would prevent drift.
10. **k8s client-go version skew** between `pvlogger-fusionsolar` and
    `pvlogger-fusionsolar-auth` for accessing the same secret.
11. **Pushgateway metrics** only in fast+full collectors; device collector,
    fusionsolar and exporter fail silently. Decide: metrics everywhere or an
    explicit reason why not.
12. **Hard-coded daily pause window** (00:00, 00:02–00:04) in
    `pv-fast-data-collector/cmd/collector/main.go` instead of the cron schedule.

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
