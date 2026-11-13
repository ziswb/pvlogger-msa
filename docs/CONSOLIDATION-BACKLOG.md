# Consolidation Backlog — pvlogger-msa

Verified divergences and defects from a full repo review (2026-08-06), with file references. When an item is resolved
with a "from now on we always do X" outcome, record it as an ADR.

**Re-verified 2026-10-19** against HEAD `46a39ca0`. Open items are under **Open**; resolved, superseded and moot
items under **Done**. Both sections are sorted by item number. Item numbers are stable IDs — never renumber or reuse
one. The old category headings (defects / Go / Kotlin / hygiene) were dropped: the legend marker on each item
carries the same information.

Legend: 🔴 likely defect · 🟡 inconsistency worth consolidating · ⚪ cosmetic.

## Next up

**Status 2026-10-26.** The CloudNativePG move (item 40) is done: since the cutover on 2026-10-25 (21:07–21:31) every
service connects to the in-cluster cluster `pvlogger-db` with TLS required, and security finding 3 is closed. Every
deployed image matches its Dockerfile LABEL (checked live 2026-10-26: data accessor 3.12.3, masterdata server
3.5.1, frontend 3.11.2, fusionsolar 3.5.0, fusionsolar-auth 3.4.1, device collector 3.7.2, fast collector 0.13.2,
full collector 0.8.2, KPI exporter 3.5.0), so no release is waiting. Since the same day, database roles are created
by CloudNativePG and migrations only GRANT (ADR-0023).

Dated items, in order:

- **About 2026-11-01, infrastructure side:** runbook part D drops the three PVLogger databases on the old server
  (`<central-host>:5434`) and its ExternalName service. The server itself stays for other homelab apps.
  Nothing here still points at it; afterwards, trim the "old server" note in the battery simulator README.
- **Next:** item 45, connection timeouts so a CNPG switchover does not hang the services (2026-11-05).
- **From about 2027-01-25:** item 42, drop the unused `modbus_response_data` indexes.

Everything else open here is low priority hygiene (items 23, 30).

### Status 2026-10-21 (end of the 2026-10-20 session)

**Released for build, all versions in the Dockerfiles:** data accessor 3.12.1, masterdata server 3.5.1, frontend
3.11.2, fusionsolar 3.4.1, KPI exporter 3.5.0; and, rebuilt on Go 1.27.3 with no code change, fusionsolar-auth
3.4.1, device collector 3.7.1, fast collector 0.13.2, full collector 0.8.2. The operator builds them all with
`scripts/build-all.sh` and redeploys `pvlogger.yaml`. Chart tags (infrastructure `values.yaml`) that must move with
it: `fusionsolarAuth` 3.4.1, `pvDeviceDataCollector` 3.7.1, `pvFastDataCollector` 0.13.2, `pvFullDataCollector`
0.8.2, `kpiExporter` 3.5.0. *Deployed 2026-10-21 00:26* — every Deployment and CronJob runs the versions above
(checked with `kubectl`). Note: a build started before the version bumps overwrote the tags fusionsolar-auth 3.4.0,
fast collector 0.13.1, device collector 3.7.0 and full collector 0.8.1 with Go 1.27.3 rebuilds of the same source;
`build.sh` now refuses that without `-f` (`ebe4159d`). *Verified 2026-10-21 18:50:* every pvlogger Job since the
deploy completed (no failed Jobs, no Warning events); fast and full collectors store KPIs and push metrics, the
device collectors, fusionsolar-auth and the exporter CronJob run cleanly. The only WARN is the device collectors'
pre-existing "modbus connection already closed" (a double close, also in the runs before the deploy). *Fixed
2026-10-21*, device collector 3.7.2: `readDeviceIdentifiers` (device list + identifiers jobs) closed the connection
explicitly and again by `defer`; the explicit close is gone and `TcpHandler.Close` is idempotent now (tests in
`pvlogger-shared-modbus/tcp-handler_test.go`). Fast and full collectors pick up the idempotent `Close` with their
next release; it changes nothing for them.

**Receiver export (ADR-0021, infrastructure ADR-0024) — switched 2026-10-23.**
- Done here: the exporter's `serve` and `relay` modes, consumption fix, multi-arch image, install guide
  (`pvlogger-kpi-exporter/README.md`). Brief with the contract (service/port 8080, `GET /v1/export/latest`,
  Bearer token from `KPI_EXPORT_TOKEN_FILE`) handed to the infrastructure side.
- Infrastructure side done 2026-10-21 00:28 (infrastructure `c9330d4`): `serve` Deployment, Tailscale ingress
  `pvlogger-export` (`tag:pvlogger-export`), token Secret, tailnet policy granting `tag:pvlogger-relay` only
  `tag:pvlogger-export:443`. Checked from the Mac: no/wrong token 401, token 200. Left: the single-use auth key for
  `tag:pvlogger-relay`, created just before the install.
- Relay tested 2026-10-21 as the systemd unit on an x86_64 Ubuntu laptop: tsnet join, `/export` 200, file written,
  rejoin after restart without the key (README).
- Operator: tell him `pv_verb_akt_w` changed (export + import → measured consumption; reaches his OneDrive files
  with 3.5.0 already); then install the relay and run it in parallel with OneDrive until he switches. His answer
  (2026-10-21): `x86_64`, no Docker or Podman, so the relay runs as a systemd service from an amd64 binary
  (`pvlogger-kpi-exporter/systemd/`, README; infrastructure ADR-0024 amendment 2026-10-21). Afterwards the CronJob mode and the OneDrive volume go.
- *Done 2026-10-23* (infrastructure `docs/RESILIENCE-PLAN.md`, phase 5): he installed the relay himself and reads
  from it; his OneDrive client is off, ours is paused. *2026-11-05:* our client and the CronJob removed; item 44 not pursued (agreed with him).

**`fsdata` migrations (item 29) — done 2026-10-21**, fusionsolar 3.5.0 (ADR-0022): deployed, baseline applied on
.2 (`tern_schema_version` = 1). Rerun procedure: `infrastructure/docs/runbooks/pvlogger-fsdata-migrate.md`.

**Security findings still open:** 3 (closes with `POSTGRESQL_SSLMODE=require` at the CloudNativePG cutover), 15
(pool size, speculative). 18 closed 2026-10-23, 3 closed 2026-10-25.

### Status 2026-10-23

- **Data accessor 3.12.2** (item 41): `read-latest-register-value` runs as two single-table statements, no
  join. *Deployed 2026-10-23 12:07* (confirmed by the infrastructure side at 15:35): the route answers 200 and the
  fast collector completes every minute. Median latency ~5 ms against ~2 ms on 3.12.1 (two round trips instead of
  one), accepted by the operator.
- **`build.sh` pushes only the version tag** (infrastructure backlog 58, ADR-0003): no more `:latest`.
- **Security finding 18 closed:** the Tailscale Ingress no longer reaches the frontend's `/metrics` (infrastructure
  `6882012`, live 14:17). The same deploy runs the three JVM pods hardened (UID 1000, no capabilities, no privilege
  escalation, RuntimeDefault seccomp); the images already ran as UID 1000 and write nothing outside `/tmp`.
- **Hardening round 2 live 15:12** (infrastructure `3108ad3`, security finding 25 there): the Go jobs run as UID
  3010, the KPI exporter (CronJob and `serve`) as 3011 with `fsGroup` 3011 for its 0440 token, the mock server as
  1000; no capabilities, no privilege escalation, RuntimeDefault seccomp. First runs succeeded (fast collector,
  FusionSolar devkpi/stnkpi/auth, exporter write and prune, export endpoint 200). Not yet run under the new
  settings: the nightly device collectors, `db-backup`, the full collector, dev5minkpi and FusionSolar
  stnday/month/year — the operator checks them. *Checked 2026-10-26:* the device collectors, the full collector,
  dev5minkpi and `db-backup` (after two fixes, infrastructure backlog 89) have succeeded since. FusionSolar
  stnday/month/year last ran before the change; their next runs are 2026-11-05, -06 and -07.

## Open

23. **Tiltfile rot**: references non-existent `pvlogger-infrastructure/` paths, no
    `docker_build` steps. Decision (2026-08-07, ex open question 8): **fix, don't remove** — Tilt is the intended dev
    loop for larger changes. Low priority; pairs with the mock-server refresh (item 30).

    *Re-verified 2026-10-19:* unchanged — the Tiltfile still loads `pvlogger-infrastructure/...` manifests and has
    no `docker_build`.
30. **Refresh the outdated mock server** (low prio, ex open question 8): unused for a while and out of date. Update it
    so the Tilt environment (item 23)
    works again for larger changes, and consider building e2e tests around it — none exist today. It stays local-only,
    never production (open question 6).
42. ⚪ **Drop the unused `modbus_response_data` indexes — a quarter after the CloudNativePG cutover** (split out of
    item 41 on 2026-10-23; the cutover it waited on ran 2026-10-25)

    Candidates, all at 0 scans on .2 (2026-10-20), ~1.3 GB together:
    - The timestamp indexes `date_created`, `request_time`, `response_time` (398 MB each).
    - `modbus_response_data_dev_id_idx` (124 MB). No composite leads with `logic_device_id`, so it is not redundant
      by construction; like the timestamp indexes, only usage counters can show it is unused.

    Why wait: dropping rests on usage counters alone, so they must cover a full quarter, long enough to see monthly or
    quarterly reporting paths. The counters on .2 started 2026-08-08, but `stats_reset` is empty and the pre-V11
    plans' indexes also read 0, so they were reset some time after 2026-08-26 — how far back they reach is unknown.
    The CloudNativePG cluster starts its own from zero, so the quarter starts at the cutover (2026-10-25: due
    from about 2027-01-25). Then: read `idx_scan`
    and `last_idx_scan` for the four indexes on the new cluster, drop those still at 0 in a new Flyway migration
    (`DROP INDEX` is a catalog change, no manual step, like V12).

    *Watch (not a task):* if an I16/U16 register ever joins the dongle block, `modbus_register_short_values` and
    `modbus_register_ushort_values` need the same `(register_address, response_id)` index first. V11 left them out
    on purpose, and they have no index besides the primary key, so since 3.12.2 the second lookup statement would
    read the whole table. Today the KPI lookup reads only the I32/U32 registers 37498–37516.

45. 🟠 **Services hang on dead database connections after a CNPG switchover** (added 2026-11-05, reported by the
    infrastructure side; its backlog 96, "Draining a server")

    On 2026-11-05 the operator drained node05 for maintenance. CloudNativePG switched the primary from
    `pvlogger-db-2` (node05) to `pvlogger-db-1` (node06) at ~10:28 and the node was powered off. The data accessor
    (moved off node05 by the same drain) then answered requests only by timing out: the KPI endpoint got `no route
    to host` and `context deadline exceeded` from it, every fast collector run failed from 10:24 to 10:41, and its
    `/metrics` liveness probe failed until the kubelet restarted it twice (the second at 10:40). After the second restart it
    worked at once. The masterdata server restarted once as well.

    Likely cause (not reproduced): the pooled connections pointed at the old primary's pod IP, which vanished with
    the node, so no RST came back. `DatabaseFactory` sets no `socketTimeout`, `connectTimeout` or `tcpKeepAlive` on
    the pgjdbc URL and no Hikari `keepaliveTime`/`validationTimeout`; with pgjdbc's default `socketTimeout=0` a read
    on such a connection blocks until the kernel gives up on TCP retransmits (~15 min), and every pool slot fills
    with blocked calls.

    To do: in `pvlogger-shared-database`, set pgjdbc `connectTimeout`, `socketTimeout` (above the longest legitimate
    query, e.g. the full collector's batch) and `tcpKeepAlive=true`, and Hikari `keepaliveTime` and
    `connectionTimeout`, so a dead connection fails within seconds and the pool replaces it. Then verify with a
    planned switchover (`kubectl cnpg promote` or a drain) while the fast collector runs: at most one failed run.

    *Status 2026-11-05:* fix live (Helm revision 63 at 10:59, infrastructure commit f42f646). Data accessor 3.12.4 and masterdata server 3.5.2:
    pgjdbc `connectTimeout=5` and `tcpKeepAlive=true` on every URL; `socketTimeout=60` on pooled connections only
    (Flyway keeps no read limit, so a long migration is not cut off); Hikari `connectionTimeout` 10 s,
    `validationTimeout` 3 s, `keepaliveTime` 30 s. Fusionsolar 3.5.1 (and the battery simulator) add pgx
    `connect_timeout=10`; they open a fresh connection per run, so they never had the stale-pool problem.
    `tcpKeepAlive` uses the kernel defaults (first probe after 2 h), so in practice the socket timeout and
    Hikari's keepalive do the work.

    *Clean promote passed 2026-11-05 11:06:31* (`kubectl cnpg promote`, pvlogger-db-1/node06 → pvlogger-db-2/node05):
    the next data accessor request found its 3 pooled connections closed, Hikari dropped them and answered 200 in
    5.3 s on a new connection; masterdata's housekeeper dropped its 3 in the background; no restarts, no failed
    fast collector run. A clean promote closes the sockets, so this did not exercise `socketTimeout`. **Open until
    the next drain + power-off of the primary's node** (the case from 10:23): pass = no kubelet restart of the data
    accessor, at most one failed fast run, DB errors (`An I/O error occurred`, SQLState 08006) for at most ~60 s.

## Done (resolved, superseded or moot)

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
    so the 00:02–00:04
    window protects the *device jobs from the fast collector* — it now lives in the CronJob schedules
    (`* 1-23 * * *` + `0-1,5-59 0 * * *` in the Helm chart), `main.go` has no time logic, and monitoring treats the pair
    as one logical job. Follow-ups since resolved by decision: the **full collector's** ADR-0014 violation (`Produced`
    from register 32114's latest value) dissolves under **ADR-0016** — its private KPI path gets deleted, not aligned
    (item 34) — and the two-CronJob fast pair is superseded by the uniform hourly window (item 36). Still optional:
    `TcpHandler`
    reconnect-between-attempts hardening.

    *Kept in Done 2026-10-19:* the `TcpHandler` hardening is optional and has no item of its own; open one if
    reconnect failures ever show up.
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
    client-side route appears). Extraction into a shared module remains item 15.
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
22. ~~**Test coverage very uneven**~~: `pv-full-data-collector` and the shared Kotlin `-rest`/`-database` libs have zero
    tests. (The Angular SPA turned out to have 47 Karma specs — the "frontend has one" here matches the Ktor side,
    which has a single test class. Corrected 2026-08-16; since item 37 the Karma specs gate the frontend image build.)

    *Re-verified 2026-10-19:* still no tests in any of the three modules. Later the same day `-database` got its
    first (`DatabaseFactoryTest`, item 40); `pv-full-data-collector` and `-rest` remain.

    *2026-10-23:* `-rest` got `KpiValuesDtoTest`: JSON round trip, the 12 field names as the wire contract (the
    frontend's client ignores unknown keys, so a renamed field would silently read 0.0; the KPI exporter's Go
    struct carries the same names), strict decoding of the wire form, zero defaults. `pv-full-data-collector`
    remains.

    **Resolved 2026-10-23.** `pv-full-data-collector` got `dynamic-data-collector_test.go`. Its own code is only the
    flow "read the sweep, then aggregate that run" (no job switch); the reading and the KPI math live in
    `pvlogger-shared-modbus`, which has tests. The flow now takes its two steps as functions (`runSweep`, no behaviour
    change, no release needed), and the tests pin: the aggregator gets the run that was read, a failed read never
    aggregates, an aggregation error is returned (non-zero exit), and the sweep reads DYNAMIC + ALARM.
24. ~~Committed IDE files (`.ipr`, `.iws`, `.iml`), `.DS_Store`; simulator output files (`summary-2024.*`) committed at
    module root.~~

    **Resolved 2026-08-16.** Untracked all 12 `.iml`/`.ipr`/`.iws` files (kept on disk) and added `*.iml`/`*.ipr`/
    `*.iws`/`.DS_Store` to `.gitignore`. Scope decision: the `.idea/` directory itself **stays tracked** (operator
    choice — only the files the item names were removed). Two parts of the item were already stale: no `.DS_Store`
    was ever tracked (only present on disk), and the `summary-2024.*` simulator outputs no longer exist in the tree
    or the index. Files remain in git history — nothing sensitive (internal LAN hostnames only, already in docs).
25. ~~`pvlogger-frontend/src/web/README.md` is stock Angular-CLI boilerplate citing the wrong version.~~

    **Resolved 2026-08-16.** Rewritten in place (operator choice over module-root move or deletion) as a real README:
    Angular 19, dev-server proxy targets, and — the load-bearing part — the committed-build-output workflow
    (`ng build` emits into `src/main/resources/static/`, which is checked in; Gradle/Docker never runs npm, so SPA
    changes must be committed together with the regenerated static files). *That workflow was replaced the same day —
    see item 37; the README was updated again with it.*
26. ~~`pvlogger-database/migration/irrelevant/` — delete or archive; the PG18 migration guide is a finished runbook and
    could move to a `docs/history/`.~~

    **Resolved 2026-10-23.** `irrelevant/` deleted (19 one-off scripts from the classic → msa migration; they stay
    in git history). The PG14 → PG18 guide moved to `docs/history/postgres-migration-guide.md`. Nothing referenced
    either.
27. ~~**Site coordinates hard-coded in sunrise-sunset**~~

    **Superseded 2026-08-07**: the service turned out to be deprecated (ADR-0005), so the hard-coded coordinates (the
    operator's home — personal location data in repo and image) disappear with the module. Folded into item 28.
28. ~~**Retire `pvlogger-sunrise-sunset`**~~ (deprecated per ADR-0005: it gated FusionSolar collection to daylight
    hours, and that gating is no longer wanted).

    **Module deleted 2026-08-27.** 26 tracked files gone (`Application.kt`, the Koin/Ktor configuration, the
    `SunriseSunsetDao`/`SolarTable` Exposed layer, the REST controller and paths, `SunriseSunsetService`, three HOCON
    profiles, `V1__InitialSchema.sql`, the Dockerfile and the tests), plus its entry in `settings.gradle.kts`, its
    `k8s_yaml(...)` line in the Tiltfile (which pointed at a path that no longer exists anyway — item 23), the
    CLAUDE.md service-map row, the CONTEXT.md service row, and the TROUBLESHOOTING.md ShadowJar affected-services
    entry. Verified before deleting: nothing in the repo referenced it (no Kotlin/Go/TypeScript imports, no REST path
    constants in `pvlogger-shared-rest`) and `kubectl get deploy,svc,cronjob -A` showed nothing deployed. With it goes
    the operator's home coordinates (ex item 27), the last per-service `HttpClient` (ADR-0018), the duplicate
    `/metrics` registration (security item 18), and the repo's only `application-test.conf`. `./gradlew build` green
    afterwards.

    **Fully retired the same day.** The operator dropped the `sunrisesunset` database (no final dump — nothing read
    those cached sun times) and removed it from `DB_NAMES` in the chart's `db-backup.yaml`; `pg_database` now lists
    eight databases with no `sunrisesunset` among them. The chart still has to be **deployed** for the live
    ConfigMap to lose the entry — until then the nightly job dumps a database that no longer exists and dies on it
    under `set -e` (infrastructure item 32).

    **One remnant left, deliberately:**
    `pvlogger-database/migration/migrate_db_aio.sh:23` still names `sunrisesunset` in the PG18 migration's database
    list. Left alone on purpose: it is the record of a migration that did include that database (item 26 covers
    archiving that directory).
29. ~~**Introduce `fsdata` schema migrations**~~ — a known gap since the Kotlin→Go rewrite of fusionsolar (rationale in
    ADR-0002, ex open question 4), not a decision.

    *Rationale corrected 2026-08-27:* this item used to read "the DDL exists only in the running DB and old dumps",
    which is stale and made the item look like a data-recoverability risk. The full DDL **is** committed, as sqlc's
    schema input: `pvlogger-fusionsolar/internal/database/schema.fsdata.sql` (209 lines — 8 `fs_*` tables with their
    primary keys and indexes). What is missing is a *runner* and any *drift detection*: nothing applies that file to
    a database, nothing verifies it still matches production, and it is maintained by hand alongside
    `query.fsdata.sql`. So the risk is silent divergence between file and live schema, not an unrecoverable schema —
    which puts this item well below where its old wording implied. Two steps:
    (1) **baseline** — verify `schema.fsdata.sql` against a `pg_dump --schema-only` of the live `fsdata` (it, not a
    fresh dump, should become migration 0001 if the two agree; reconcile first if they do not); (2) **wire a
    runner** — recommended: a `migrate` job inside `pvlogger-fusionsolar` itself (job-dispatch pattern; no new service,
    per ADR-0007) using tern (pgx ecosystem) with `go:embed`-ded SQL files, executed manually as a plain K8s Job. The
    operator's original plan — a dedicated manually-activated migration service — would work but fails the ADR-0007 bar;
    keeping migrations next to the sqlc queries they must match is the decisive argument for the in-module job.

    *Note 2026-10-19:* infrastructure ADR-0022 copies `fsdata` into CloudNativePG with `initdb.import`. Do
    step (1) before that cutover, against the database being copied — afterwards the live schema is whatever the
    import produced, and the old server is the only reference left.

    *Step (1) done 2026-10-20* against `pg_dump --schema-only -d fsdata` on .2. Same 8 tables, same columns, keys,
    foreign keys and indexes (`serial` vs. integer + sequence is the same thing). Three differences:
    - **`fs_response.raw` is `text` live, `jsonb` in the file.** sqlc maps it to `[]byte` either way, so the code
      works against both — but a database built from the file would reject a reply that is not valid JSON, and
      live would not. *Reconciled 2026-10-20:* the file now says `text`, matching production (converting live
      would rewrite the table and fail on any row that is not JSON). sqlc regenerated (`Raw` is now `string`); the
      repository converts the JSON bytes at its one call site.
    - **A `flyway_schema_history` table** in `fsdata`, left over from the Kotlin era (open question 4). Not in the
      file, nothing reads it. Its rows are the only record of the old migrations; keep or drop when the runner is
      wired, but don't let tern's own history table be confused with it.
    - **A leftover publication `upgrade_pub` (FOR ALL TABLES)** from the PostgreSQL 18 upgrade by logical
      replication — the same leftover infrastructure backlog 86 found and dropped in `immich`. Not schema, but the
      CloudNativePG import carries it along (and the restore runbook shows it producing `wal_level` warnings).
      *Dropped 2026-10-20* by the operator in `pvdata`, `pvmasterdata` and `fsdata` (it existed in all three) as
      `postgres` on .2; `pg_replication_slots` was empty in each, so no WAL was being retained.

    *Step (2) done in code 2026-10-21* (ADR-0022), fusionsolar 3.5.0: tern with embedded SQL in
    `internal/database/migrations/`, job `JOB_NAME=MIGRATE` (needs only the DB env, no token). `001_baseline.sql`
    is the verified schema with `IF NOT EXISTS`, so on .2 it changes nothing and records version 1 in
    `public.tern_schema_version`; `flyway_schema_history` stays untouched. sqlc now reads `migrations/` as its
    schema and `schema.fsdata.sql` is gone (generated code unchanged). Tested on PostgreSQL 18: fresh database →
    0 → 1, rerun "up to date"; a copy of the production schema with a non-JSON `raw` row and a Flyway table → 0 → 1,
    data and Flyway rows untouched; both end with identical `pg_dump -s`. **Left (operator / infrastructure):**
    build and deploy 3.5.0, add a manual Job to the chart (same image and env as the fusionsolar CronJobs,
    `JOB_NAME=MIGRATE`), run it once on .2 before the CloudNativePG import, check `tern_schema_version` = 1.

    **Resolved 2026-10-21.** fusionsolar 3.5.0 deployed 22:19; the baseline ran 22:27 as a one-off Job built from the
    `devkpi` CronJob: `0 → 1`, `tern_schema_version` owned by `pvlogger`, Flyway row and data untouched, the
    following collection runs complete. Repeatable procedure (also for later migrations, and as a check after the
    CloudNativePG import): `infrastructure/docs/runbooks/pvlogger-fsdata-migrate.md`. No chart template — the runbook
    builds the Job from the CronJob, so it cannot drift from the CronJobs' env.
31. ~~🔴 **All 22 data-accessor tests fail on H2**~~

    **Resolved 2026-08-07**
    (ADR-0009): the tests now run against real PostgreSQL 18 via Testcontainers — V6 was PostgreSQL-only syntax and
    unfixable on H2 without a checksum-breaking edit. The move also exposed and fixed a test asserting UTC day-bucketing
    that production (TIMESTAMPTZ, `TZ=Europe/Zurich`) never had. Extended same day: masterdata tests moved to
    Testcontainers too, the H2 dev profiles now point at the local compose PostgreSQL, and H2 is removed entirely — no
    migration needs H2 compatibility anymore. Tests require a running Docker daemon.
32. ~~**Repo-wide `gofmt -w`**~~: ~13 Go files fail `gofmt -l` (import ordering, struct-field alignment) — pre-existing,
    surfaced when golangci-lint was first run locally (2026-08-09; its gofmt linter is not enabled, so lint stays green
    regardless). One mechanical formatting commit, ideally with nothing else in it; consider enabling the gofmt/gofumpt
    linter in the golangci-lint config afterwards to keep it that way.

    *Re-verified 2026-10-19:* down to 7 files — `pv-device-data-collector/cmd/collector/main.go`, both
    fusionsolar `internal/config/config.go`, `pvlogger-fusionsolar/internal/fusionsolar/device_5_minutes_kpi.go`,
    `pvlogger-kpi-exporter/internal/kpi/export_test.go`, `pvlogger-shared-modbus/metrics/metrics-service.go`,
    `pvlogger-shared-modbus/registers/masterdata_drift_test.go`.

    **Resolved 2026-10-21.** The 7 files formatted in a commit of their own (imports, field alignment; no code
    change). A root `.golangci.yml` (golangci-lint v2) enables the `gofmt` formatter for every module, since
    golangci-lint finds the config in the parent directory; checked that a misindented file fails lint. The
    top-level `make lint` (and so `./gradlew makeGoLint`) now also covers `pvlogger-fusionsolar-auth` (left out of
    `SUB_DIRS` since it was added) and the libraries `pv-logger` and `pvlogger-shared-modbus` (`LIB_DIRS`, lint
    only, no Makefile of their own); the two `pvlogger-shared-modbus` files had slipped through for that reason.
    All 9 modules: 0 issues.
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
    (infrastructure backlog 2) was resolved **by removal**: ADR-0016 abolished the toggle, so both `suspend:` lines and
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
37. ~~**Angular build output committed to git; separate `build-angular.sh` step**~~

    **Resolved 2026-08-16.** The SPA is now compiled inside the frontend Docker image build (Dockerfile stage 1:
    `node:22-alpine`, `npm ci`, Karma tests, `ng build`) and copied into the fat-JAR stage — `src/main/resources/static/`
    is untracked/ignored and `scripts/build-angular.sh` is deleted. The old split's motivation (image build shouldn't
    pay the npm/Chromium cost for a rarely-changing SPA) is preserved by layer caching: the Angular stage's `COPY`s see
    only `src/web/`, so unchanged web sources make the whole stage a cache hit (cache is per-machine; a pruned cache
    costs one ~2–3 min rebuild). Fixed alongside: `npm ci` instead of `npm install`, Node 22 LTS instead of
    unsupported 25 (+ `engines` in package.json), `build:ci` npm script as single source of the CI command,
    `angular.json` outputPath → normal `dist/web`, and `.dockerignore` now excludes `.git`/`**/node_modules`/
    `**/.angular`/`**/build` etc. — the context every image build uploads shrank from ~3.5 GB to ~30 MB. Frontend 3.10.0.
38. ~~🔴 **Planner statistics rot silently: `read-latest-register-value` takes 29.6 s and races the fast collector's
    timeout**~~ (observed 2026-08-26; resolved 2026-08-26 by V10, leftovers in item 41)

    `pv-fast-data-collector` jobs fail intermittently — several per hour — with
    `context deadline exceeded (Client.Timeout exceeded while awaiting headers)` on
    `GET /v1/read-latest-register-value/I32/100/37516/<run>`, surfaced as the misleading
    "no value for register 37516 … the run must store the dongle aggregate block before aggregating". The message
    points at item 35's ordering invariant; the actual cause is unrelated and lives in the database.

    The accessor's own request log shows a perfectly deterministic split: **every** `I32` read of the dongle block
    (37516, 37502) takes 29 134–29 889 ms, **every** `U32` read (37500) takes 5–91 ms. The time is spent inside
    PostgreSQL, CPU-bound with no wait event — `RegisterReader`'s query
    (`SELECT register_value … JOIN modbus_response_data … WHERE register_address = $1 AND logic_device_id = $2 AND
    job_id = $3 ORDER BY response_time DESC LIMIT 1`) planned as:

    ```
    Nested Loop (actual time=31565.430..31565.431)
      ->  Index Scan reg_addr_idx   rows=16554  loops=1
      ->  Index Scan dev_id_idx     rows=1      loops=16554   -- 16 554 probes, 16 552 rows discarded each
    ```

    Root cause: `pvdata` column statistics were last collected **2026-04-28/29**. The dongle aggregate block
    (`logic_device_id = 100`, registers 37502/37516 — item 35, ADR-0017) only went live **2026-08-14 17:13**, so the
    MCV list for `modbus_response_data.logic_device_id` was still `{1,16}` with frequencies summing to 1.0, and
    `register_address`'s 29-value MCV list predated 37516. The planner therefore estimated **1 row** for both
    predicates (`cost=0.44..8.46 rows=1`) where reality is 16 557 each, and built a 1×1 nested loop that executes
    16 557 × 16 557 ≈ **274 M** iterations. `modbus_response_data_dev_id_idx` had logged 3.0 **trillion** tuple reads.
    Forcing any other join strategy runs the same query in **6.8 ms** — a 4 600× difference from row estimates alone,
    with no change to indexes, schema or code. U32 escaped only by luck: identical 1-row fantasy, but the cost tie put
    the materialised side on the outside, so each table is scanned once.

    Why it never self-healed: `autovacuum_analyze_scale_factor = 0.05` on a 34 M-row table
    demands ~1.7 M modifications, while these tables grow a few thousand rows a day — the next autoanalyze was ~11
    months out. A scale-factor threshold assumes churn is proportional to table *size*; for append-only fact tables it
    is proportional to *time*. Every large table in `pvdata` carries that trap.

    **How likely is a recurrence? Narrower than first written (corrected 2026-08-27, operator input).** An earlier
    draft of this item claimed "**any** future new logic device, register range or device type will silently produce
    catastrophic plans" — which over-reads the trigger. The dongle logic device has existed since the start of the
    project; what changed on 2026-08-14 was only that we *started reading it over Modbus* (its Modbus reachability
    was unknown before), and the operator expects **no new devices in future**. So the remaining trigger is a single
    one: a **`register_address` value that is new to the MCV list**. That is not hypothetical — the fast collector's
    hard-coded KPI set changed twice in August (32080/37113 retired, 37498/37500/37502/37516 added, items 33/34) and
    a masterdata CSV import can introduce addresses for the full sweep — but it is rare, operator-initiated, and V10
    caps the exposure at 1-3 days rather than ~11 months.

    **Runbook consequence, which is the cheap half of the remaining fix:** after changing a collector's register set
    or importing masterdata, run `ANALYZE modbus_response_data, modbus_register_integer_values,
    modbus_register_uinteger_values;` on `pvdata` instead of waiting out V10's 1-3 day window. That covers the same
    ground as the two-statement application change discussed under the follow-ups below, which is therefore
    downgraded to cheap insurance to fold in the next time `RegisterValueDao` is touched — not standalone work.

    **Fixed** by `pvlogger-modbus-data-accessor/src/main/resources/db/migration/V10__FixAutoanalyzeThresholds.sql`:
    flat `autovacuum_analyze_threshold = 5000` with `autovacuum_analyze_scale_factor = 0` on the seven large
    append-mostly tables (1–3 day cadence; ANALYZE samples 30 000 rows regardless of table size, so the cost is
    negligible), plus a one-shot `ANALYZE` of the same tables so deploying the migration repairs an already-rotted
    database. The two small tables (`modbus_characteristic_curve_data`, `modbus_register_string_values`) are left on
    the scale factor, which is *tighter* for them. **Prod needs `ANALYZE` run manually (or the accessor restarted) to
    fix the live incident — the migration only applies on the next data-accessor start.**

    **Release:** data accessor `3.10.2` (patch — migration only, no code change), chart tag bumped to match in the
    infrastructure repo. Deploy: `scripts/build.sh` the accessor image → `ansible-playbook playbooks/pvlogger.yml`,
    which applies V10 on pod start. Verify afterwards that
    `read-latest-register-value/I32/100/37516` drops from ~29.6 s to single-digit ms in the accessor log, that
    `pv-fast-data-collector` stops producing failed jobs, and that central-server CPU falls once the 274 M-iteration
    loop stops running every minute.

    **Verified 2026-08-26 after the 3.10.2 rollout.** V10 applied (flyway history row 10, all seven tables carry the
    reloptions, ANALYZE at 21:55:57). The route's latency fell from 29 134-29 889 ms to 7-401 ms and the failed jobs
    stopped. The plan is now 0.99 ms end to end — a 32 000x improvement on the 31 565 ms measured before:

    ```
    Nested Loop
      -> BitmapAnd on modbus_response_data
           Bitmap Index Scan on modbus_response_data_job_id_idx
           Bitmap Index Scan on modbus_response_data_dev_id_idx
      -> Index Scan using modbus_register_integer_value_idx
           Index Cond: (register_address = 37516 AND response_id = ...)
    ```

    That plan **corrects two claims made below before the rollout**, which are kept here only because the corrections
    are the useful part:

    - `modbus_register_integer_value_idx` is **not** dead weight and must not be dropped. `V4__ModifyIndexes.sql`
      created it for exactly this access pattern and it now serves it with an equality cond on both columns. Its 0
      scans were a symptom of the misestimate, not evidence of a useless index — with `register_address` estimated at
      1 row the planner never needed it. **The real gap is that the symmetric index is missing on the other three
      register-value tables** (`uinteger`, `short`, `ushort`), so those still fall back to a hash join or a filter.
      Adding `(register_address, response_id)` there is the remaining structural fix — shipped as V11, though see the
      correction below about what it does and does not buy. *(2026-10-19: V11 indexed only `uinteger`; `short`/
      `ushort` are never read on this path — see V11's header.)*
    - The index-usage evidence was **too short to justify dropping anything**. `stats_reset` is NULL but the
      postmaster only started 2026-08-08, so the counters cover ~19 days — blind to monthly or annual reporting
      paths. Two of the "0 scan" entries were primary keys, which are not droppable anyway. The genuine candidates
      shrink to `modbus_response_data`'s three timestamp indexes (`date_created`, `request_time`, `response_time`,
      398 MB each), and only after a full quarter of counters.

    Follow-ups as recorded at the time (**the open ones moved to item 41, and on 2026-10-23 to item 42**):

    - **`job_id`'s selectivity is still underestimated 10x.** It is now used (as one half of a BitmapAnd) but the
      planner still estimates 191 rows where the true figure is 1: `n_distinct` is 97 343 against 1 003 220 actual
      distinct job_ids across 18.5 M rows — the classic ANALYZE underestimate for a high-cardinality `text` column,
      which more frequent ANALYZE cannot fix. The wasted half of that BitmapAnd reads 16 704 index entries and costs
      204 of the plan's 210 estimated units. `ALTER TABLE modbus_response_data ALTER COLUMN job_id SET STATISTICS
      1000` should drop the dev_id half entirely. Worth doing for robustness as much as speed: `job_id` selectivity
      comes from a scalar `n_distinct`, which degrades gracefully, whereas the predicates that caused this outage
      (`logic_device_id`, `register_address`) are estimated from MCV lists that fail catastrophically the moment a
      brand-new value appears. Steering the plan onto `job_id` makes it immune to the exact failure mode above, which
      V10 only narrows to a 1-3 day window rather than closing.
      A unique index on `(job_id, logic_device_id)` would pin the estimate at 1 row but is **not** available: the max
      is 15 rows per pair and 1 956 540 of 1 981 639 groups have more than one, so the `ORDER BY response_time DESC
      LIMIT 1` is load-bearing rather than defensive.

      **Correction (2026-08-26), and it matters for how this class of bug gets fixed.** The claim above that an index
      makes the plan "robust by construction" is **false, and was tested**. Reproduced on a scratch PG 18.6 with the
      MCV blind spot recreated deliberately (200 k rows for devices 1/16, ANALYZE, then 16 557 device-100 rows added
      without re-analyzing): the catastrophic plan runs in 15 648 ms without `modbus_response_data_job_dev_idx` and
      **15 257 ms with it** — the planner ignores the new index and keeps the same nested loop. It is right to, by its
      own lights: when every predicate is estimated at 1 row, every join order costs ~16 units, the winner is an
      arbitrary tie-break, and no index can undercut a plan that already looks free. **That tie-break is the entire
      explanation of the I32-vs-U32 asymmetry** — I32 lost it and paid 29.6 s, U32 won it and paid 5 ms; indexes were
      never the difference, and neither was the register type. So no index and no statistics target confers immunity
      here. Only current statistics (V10) prevent it, and the residual 1-3 day exposure can only be closed by removing
      the join-order choice from the query — having the accessor resolve the response row and then the register value
      in two statements. That is the real remaining item, and it is an application change, not a migration.

      V11 is therefore justified on ordinary efficiency grounds instead, which are real: both lookups currently scan
      ~16 700 rows to return one, and that scan is linear in the dongle block's accumulated history (~1 170 rows/day
      — ~16.7 k now, ~430 k in a year, three times a minute forever). Measured on prod: I32 0.99 ms / 38 buffers with
      a useless 16 704-entry dev_id bitmap half; U32 2.63 ms / 293 buffers bitmap-scanning all 16 727 rows for
      register 37500 and join-filtering them. V11 makes both flat two-column equality lookups.
    - ~~**The misleading error**: a transport failure was folded into item 35's aggregation-ordering message.~~
      **Resolved 2026-08-26.** `GetJSON` now returns a wrapped `persistence.ErrNotFound` sentinel for a 404 — the
      only status that can mean the row is genuinely absent — and `KpiAggregator.registerValue` keeps the ordering
      hint for that case while reporting everything else as "could not read register N ... from the data accessor".
      Regression-tested in both directions (`TestKpisFor_TransportFailureIsNotBlamedOnOrdering` reproduces the
      original message against the pre-fix code and fails on it). Fast collector `0.13.1`, full collector `0.8.1` —
      both link the shared aggregate path. The 30 s client timeout is deliberately left alone: the query is the thing
      that was wrong, not the timeout.
    - ~~**Nothing scraped the services**, so the slow layer was invisible.~~ **Resolved 2026-08-26** in the
      infrastructure repo: `prometheus-servicemonitor-pvlogger.yaml` scrapes all three Ktor services' Micrometer
      endpoints, plus `PvloggerServiceRequestSlow` (p95 > 5 s for 10 min) and `PvloggerServiceTargetDown`. Note
      `ktor_http_server_requests_seconds` is a *summary* with client-side quantiles, not a histogram, so
      `histogram_quantile()` does not apply. Supersedes the ServiceMonitor half of the role's `IMPROVEMENTS.md` 16.
    - **infrastructure repo:** the data-accessor's CPU limit is `100m` with `requests == limits`
      (`pvlogger_helm_deploy/chart/values.yaml`). Not implicated here — the pod idles at 2 m and the wait was entirely in
      PostgreSQL — but tight for a JVM service. *(Split the same day on the infrastructure side: request `100m`,
      limit `1000m`.)*

    **Moved to Done 2026-10-19:** the outage is fixed and verified. The follow-ups above that are still open moved to
    item 41.
39. ~~🔴 **`/livedata` goes silent for the sessions that stay connected** (observed 2026-09-05)~~

    **Resolved 2026-09-05 (ADR-0019).** The frontend's hand-rolled WebSocket fan-out
    (`plugins/WebSockets.kt`: `MutableSharedFlow` without replay + `NotificationProducer` +
    `NotificationProducerScheduler` with an `AtomicInteger` behind a `synchronized` block) had three defects:
    - **The shared polling job was launched in the first client's session scope**
      (`scheduler.start(this)` inside the `webSocket { }` block). When that client disconnected while another was
      still connected, `stop()` only decremented the counter — but the session scope was cancelled anyway, killing
      the job. Every remaining browser then received nothing until the *last* one disconnected. Two tabs, or a
      reload while a second device watches, were enough to trigger it.
    - **60 s initial delay and no replay**: a fresh connection saw no live values for a minute, and a session
      shorter than that received none at all.
    - **A data-accessor exception ended the producer flow for good**, and a slow subscriber back-pressured the
      shared `emit`, stalling all other sessions.

    Replaced by `service/LiveKpiPublisher`: `shareIn(application, SharingStarted.WhileSubscribed(5 s, 60 s),
    replay = 1)` over a `retryWhen`-guarded poll flow, collected `.conflate()`d per session. The
    "no clients → no data-accessor traffic" property is kept (that is what `WhileSubscribed` *is*), the loop now
    belongs to the application rather than to a client, and a joining browser gets the cached value at once.
    `DataAccessorRestProxy` also caps the latest-KPI read at 15 s — the shared `PvHttpClient` default is 600 s,
    sized for the collectors' bulk writes, which is well past the next poll.

    Diagnosis note for the future: Ktor's CallLogging writes its `101 Switching Protocols: GET - /livedata in
    <n>ms` line when the session *ends*, and `<n>` is the session's lifetime — an open WebSocket produces no log
    line at all, so that entry is never evidence of a slow upgrade.

    **Follow-up 2026-09-06 (frontend 3.11.1): the fix exposed a latent race in the SPA.** `pushData` in
    `kpi-fetch-service.service.ts` deduplicated with a monotonic high-water mark (`createdAt <=
    maxCreatedAtReceived` → drop). The day's history (`GET /v1/daydata`) and the live socket are two independent
    requests; while the socket stayed silent for 60 s the history always won that race, so the mark was only ever
    advanced by data already plotted. With the socket answering in milliseconds it now wins, sets the mark to the
    newest timestamp of the day, and every history row arriving right after is older — the whole day was dropped
    and the dashboard started empty, filling one live point per minute. Dedup is now by seen timestamp (a `Set`),
    which is what the guard was actually for; `publishData` keeps a separate newest-published row so a history
    tail arriving after a live value still redraws the charts without moving the current-value tiles backwards.
    Covered by `kpi-fetch-service.service.spec.ts`.
40. ~~🔴 **Make the services ready for the CloudNativePG move**~~ (added 2026-10-19; infrastructure ADR-0022, phase 4
    of its `docs/RESILIENCE-PLAN.md`)

    ADR-0022 moves `pvdata`, `pvmasterdata` and `fsdata` to a three-instance CloudNativePG cluster, with a dedicated
    application role instead of `postgres`, and says the services need "no change beyond a new host, port and
    role". In this repo the role is not configuration, and the bootstrap assumes a superuser:

    - `user = "postgres"` is a literal in `application-prod.conf:20` of both data-accessor and masterdata-server,
      with no env override. Switching the role needs a config change (`${DB_USER}`, like the password) and a
      release of both images before the cutover night.
    - `DatabaseFactory.connect()` (`pvlogger-shared-database/.../DatabaseFactory.kt:23-31`) first connects to
      `dbServerUrl`, which ends in `/` with no database name. The PostgreSQL JDBC driver then picks the database
      named after the user: today `postgres`, which always exists; with an app role, a database of that role's
      name, which may not exist. If the target database is missing it runs `CREATE DATABASE`, which needs
      `CREATEDB`. Point the bootstrap at an explicit maintenance database (or skip it when the target exists) and
      check the app role gets through both steps.
    - Flyway runs as the new role against the imported databases. `V10` set reloptions with `ALTER TABLE`, which
      needs table ownership, and later migrations will too. The import must leave the tables owned by the app
      role.
    - `V8` created the role `battery_sim_ro`. It will not run again (it is in the Flyway history), so the role and
      its grants must come across with the import, or the battery simulator loses read access. The simulator's
      `-db-host` default (`<central-host>`) changes with the move too.
      *2026-10-25:* cutover done (21:07–21:31); the simulator's flag defaults now point at the new cluster
      (`<db-host>:5432`, `-db-sslmode require`), README updated.
    - fusionsolar reads its user from `POSTGRESQL_USERNAME` (default `postgres`,
      `pvlogger-fusionsolar/internal/config/config.go:61`), so only the chart changes there.
    - CloudNativePG generates server certificates and serves TLS by default, which removes the server-side blocker
      of **security finding 3**. Switching the clients to TLS in the same release closes that finding for every
      deployed service.

    Test against the rehearsal import ADR-0022 plans (by day, while the old server is still live), not on the
    cutover night.

    **Progress 2026-10-19 — code done, released as data accessor 3.11.0, masterdata server 3.5.0, fusionsolar
    3.4.0.** Role and bootstrap:
    - Both prod configs keep `user = "postgres"` as the default and take `${?DB_USER}` as override, so the current
      deployment is unaffected; the chart sets `DB_USER` once the app role exists.
    - `DatabaseFactory.connect()` now connects to the target database first. Only if PostgreSQL answers SQLSTATE
      `3D000` (database missing) does it connect to the explicit maintenance database `postgres`
      (`DatabaseConfig.maintenanceUrl`) and run `CREATE DATABASE`. A missing database is no longer retried 30 × 15 s;
      a role that cannot create it fails at once, naming the missing right.
    - `pvlogger-shared-database` got its first tests (`DatabaseFactoryTest`, Testcontainers): a CREATEDB role
      creates and migrates its database; a role without CREATEDB starts on a database it owns (the post-import
      shape), including an owner-only `ALTER TABLE` like V10; a role that can do neither fails fast.

    TLS is a **setting, not a fixed mode** — the central server (.2) refuses TLS (checked from the infrastructure
    side 2026-10-19), so a hard `require` would break the release before the move:
    - JVM: `sslMode` in `DatabaseConfig` (default `prefer`, validated at startup), appended to every URL the
      bootstrap builds — target, maintenance database and Flyway. Prod configs take `${?POSTGRESQL_SSLMODE}`.
    - fusionsolar: `POSTGRESQL_SSLMODE`, default `prefer`, replaces the hard `sslmode=disable`. pgx supports
      `prefer` (verified 2026-10-19 against a server without TLS: `prefer` connects, `require` is refused).
    - Battery simulator: `-db-sslmode` flag, default `prefer`.
    - Tests: `DatabaseConfigTest` (URL building, unknown mode rejected); `DatabaseFactoryTest` shows `require` is
      refused by the non-TLS test server while every other test there connects on `prefer`.

    **Rollout, agreed with the infrastructure side 2026-10-19 — switch the role on .2 first, before the move:**
    1. Create the `pvlogger` role on `central_db`, make it owner of `pvdata`, `pvmasterdata`, `fsdata` and every
       object in them (including the Flyway history tables), and run the services with it on .2. The night cutover
       then only changes the host, as ADR-0022 promises, and the ownership requirement above is met by
       construction: three databases means CloudNativePG's monolith import, which keeps the owners it finds.
       `battery_sim_ro` and its grants must still come across with the import.
    2. Chart settings (infrastructure repo): `DB_USER=pvlogger` for data accessor and masterdata server,
       `POSTGRESQL_USERNAME=pvlogger` for fusionsolar (the two languages already used different names), and the
       `db-backup` CronJob's user in the same change — as owner, `pg_dump` works without superuser.
    3. At the cutover: `POSTGRESQL_SSLMODE=require` in the chart. `require` encrypts without checking the
       certificate; `verify-full` would need CloudNativePG's CA shipped to every client — possible later.

    **Done on .2 2026-10-19** (reported by the infrastructure side): images deployed, steps 1 and 2 applied — the
    services and `db-backup` run as `pvlogger`, which owns the three databases and their objects (runbook
    `infrastructure/docs/runbooks/pvlogger-db-role.md`). Still open: step 3 at the cutover, i.e. phase 4 of the
    resilience plan, which waits on the NVMe hardware (phase 3). Close this item after the cutover.

    **Cutover done 2026-10-25, 21:07–21:31** (infrastructure `901df73`). Step 3 is live: the chart sets
    `POSTGRESQL_SSLMODE=require` (and `PGSSLMODE` for `db-backup`) through the `db-config` ConfigMap, and pg_hba
    rejects every plaintext login (`hostnossl … reject`). Checked live by the infrastructure side at ~22:16: data
    accessor, masterdata server and a fusionsolar run all connect with TLS 1.3 (`pg_stat_ssl`), and the test backup
    succeeded. The old server `<central-host>:5434` gets no PVLogger sessions any more; its databases are dropped in
    runbook part D (around 2026-11-01). The battery simulator's defaults point at the new cluster (`38401a00`).
    Security finding 3 closed with it. **Closed 2026-10-25.**
41. ~~🟡 **Remaining follow-ups from item 38**~~ (split out 2026-10-19 so that item 38 can sit under Done)

    Item 38's outage is fixed and verified. These are its leftovers, all small:

    - ~~**Verify V11 in production.**~~ **Verified on .2 2026-10-20** (PostgreSQL 18.2, Flyway V11 applied
      2026-08-26 22:57). Both indexes valid and in use: `modbus_response_data_job_dev_idx` 237 MB, 103 896 scans;
      `modbus_register_uinteger_value_idx` 338 MB, 34 632 scans. Both `read-latest-register-value` plans are a
      nested loop of two Index Conds on the composites: I32 37516 0.11 ms / 16 buffers, U32 37500 0.07 ms / 9
      buffers (before V11: 0.99 ms / 38 and 2.63 ms / 293).
    - ~~**Drop the single-column indexes the composites make redundant**~~ (~511 MB:
      `modbus_response_data_job_id_idx` 180 MB, `modbus_register_integer_values_reg_addr_idx` 227 MB,
      `modbus_register_uinteger_values_reg_adr_idx` 104 MB — all three at 0 scans, no `last_idx_scan`) in a new
      migration V12. The composites are seen winning, so the condition from V11's closing comment is met. Ship it
      before the CloudNativePG cutover (item 40), not with it — the import then copies ~511 MB less. *Done
      2026-10-20* as V12, data accessor 3.12.0, deployed the same evening: Flyway 11 → 12 in 151 ms on .2. No
      manual step, unlike V11: a drop only changes the catalog, so its lock lasts milliseconds.
    - ~~**`job_id` statistics target**~~: moot, confirmed 2026-10-20 — `attstattarget` is unset (default) on
      `job_id` and `logic_device_id`, and the plans above no longer touch the dev_id index.
    - ~~**Two-statement lookup in `RegisterValueDao.readLatestRegisterValue`**~~ — **done 2026-10-23**, data
      accessor 3.12.2. The only change that removes the join-order choice, and with it the remaining 1-3 day
      exposure. Statement 1 reads the run's response ids for the device, newest first (`response_time`, then
      `id`), through `modbus_response_data_job_dev_idx`; statement 2 reads the register's values for those ids
      through `(register_address, response_id)`; the newest response that carries the register wins, as before.
      Neither statement joins, so stale statistics can at worst make one single-table scan slower, never
      quadratic. Done before the CloudNativePG cutover on purpose: the imported cluster starts with fresh
      statistics. `RegisterValueDaoTest` pins the result (newest carrying response, other run/device ignored,
      missing gives null) and that the lookup is two statements without `JOIN`; the latter fails on 3.12.1's
      query. The manual `ANALYZE` runbook step in item 38 is no longer needed for this route.
    - ~~*infrastructure repo:* the data accessor's `100m` CPU limit with `requests == limits` is tight for a
      JVM.~~ Already done 2026-08-26 on the infrastructure side: request `100m`, limit `1000m` (memory stays
      equal), see `infrastructure/ansible/roles/pvlogger_helm_deploy/defaults/main.yml`. Noted here 2026-10-23.

    **Closed 2026-10-23.** Everything here is done or moot. What is left can only be decided a quarter after the
    CloudNativePG cutover and moved to **item 42**: the timestamp indexes, `modbus_response_data_dev_id_idx`, and the
    I16/U16 watch note.
43. ~~🔴 **Timestamps written in the repeated DST hour are stored one hour early**~~ (added 2026-10-25, reported by the
    infrastructure side: false healthchecks.io freshness alarm 02:10–03:00 CET on 2026-10-25)

    Cause: Exposed 1.0's `timestamp()` column (`InstantColumnType.notNullValueToDB`) binds an `Instant` as a
    wall-clock `java.sql.Timestamp` in the JVM zone (Europe/Zurich), dropping the offset. In the second pass of the
    02:00–03:00 hour that wall-clock time is ambiguous and resolves to CEST, so those rows land one hour early, before
    the 02:59 CEST row of the first pass. `latestKpi()` (`ORDER BY date_created DESC`) was correct; it sorted wrong
    data. The collectors send correct epoch ms (`time.Now().UnixMilli()`), and the columns are TIMESTAMPTZ since V6.
    Affects every timestamp the JVM writes through Exposed: `house_power_kpi_values.date_created` and
    `modbus_response_data.{request_time,response_time}`. Columns filled by their `CurrentTimestamp` default are safe.

    *Code fix done 2026-10-25, released as data accessor 3.12.3 and live the same day:* new `timestampTz()` column
    (`database/schema/TimestampTz.kt`) binds the `Instant` as a UTC `OffsetDateTime`; the three tables use it. No
    migration needed. Repro test `DstFallBackKpiTest` is red on the old binding (latest = 00:59Z instead of 01:30Z,
    the prod symptom) and green on the new one; the full data accessor suite passes.

    *Data repair done 2026-10-25 on .2*, before the 21:07 cutover, so the CloudNativePG import carried it over.
    Only `house_power_kpi_values.date_created` was affected: ids 1290499–1290556 (58 rows) shifted
    `+ interval '1 hour'` in one guarded transaction; 00Z, 01Z and 02Z now hold 58 rows each with no ordering break. Undo: the same ids `- interval '1 hour'`. The `date_created` of
    `raw_kpi_register_values` and `modbus_response_data` was fine, since it comes from the database default, not
    from the JVM.

    *Left as is (found during the repair):* `modbus_response_data.request_time`/`response_time` hold the collector's
    local wall-clock time stored as if it were UTC (`ModbusDao`: `toInstant(ZoneOffset.UTC)`), so they run 1–2 h
    ahead of `date_created`. Through the same Exposed bug, the 77 rows of 2026-10-24 23:00–00:00Z (local 01:xx CEST,
    read as UTC = 02:xx CET, ambiguous) landed one more hour early. The only reader is `readLatestRegisterValue`,
    which orders the responses *of one run* by `response_time`; they are seconds apart, so neither the constant
    offset nor that past hour changes its result. Worth its own item if anything starts comparing these columns
    across runs or with `date_created`.

    **Closed 2026-10-25.** Fix live as data accessor 3.12.3, the affected rows repaired.

44. ~~⚪ **Retire the KPI exporter's `export` mode**~~ (added 2026-10-26; ADR-0021, infrastructure ADR-0024)

    Since 2026-10-23 the receiver reads from the relay (`serve` → `relay`), and the OneDrive sync that carried the
    `export` CronJob's files is paused. The CronJob still writes into its NFS volume, and nothing reads those files.
    **Due once he has removed his OneDrive side** (announced for about 2026-11-06; the infrastructure side or the
    operator says when). Then:
    - Here: drop `config.ModeExport` and its branch in `cmd/exporter/main.go`, plus whatever only it uses. Keep the
      file writing and 48-hour cleanup that `relay` mode needs on the receiver's server. Decide the new default for
      `KPI_EXPORTER_MODE` (today `export`), update the README mode table, CONTEXT.md and CLAUDE.md, and release.
    - Infrastructure side: remove the CronJob, the NFS volume `kpi-export-data-claim` and the OneDrive client on .2.

    **Won't do for now (2026-11-05).** The infrastructure side is done: he removed his OneDrive side, and the
    CronJob, its NFS volume and the OneDrive client on .2 went on 2026-11-05 (infrastructure commit 2e5da96). The
    code stays: the operator and the receiver agreed that dropping `export` mode is not worth a release while
    everything works. Reopen only if one of them asks; until then the exporter is not changed for this.

46. ~~🔴 **Dashboard in a background tab shows stale, gappy or empty charts when brought back**~~ (added 2026-11-11,
    reported by the operator)

    Observed: the dashboard sits in a background tab most of the time. When switched to, it usually shows current
    data, but sometimes the data is stale, some points are missing (a straight line from one point to the next more
    than 5 minutes later), or the charts stay empty.

    Why: the day's history (`GET /v1/daydata`) is fetched only on page load and date change; after that the charts
    grow only from the `/livedata` WebSocket, one value a minute. A background tab gets paused (Mac sleep, Chrome
    Memory/Energy Saver freezing, network change), and:
    - a reconnect gets only the server's single cached value (ADR-0019), so the missed minutes are never fetched →
      straight lines across the gap;
    - after sleep or a network change the socket can be dead without a close event, so `retry` never fires → stale;
    - a failed history fetch gives up after two retries and stays empty; and the midnight day switch (`525a3013`)
      waits for a live value, so a dead socket leaves the tab on yesterday → empty or stale.

    To do (frontend, `KpiDataSourceService`): on `visibilitychange` to visible, after the tab was hidden for at least
    ~1 minute, resync:
    - the shown day is in the past and today has moved on since → switch to today (same path as the datepicker);
    - the shown day is today → re-fetch `/v1/daydata` and merge it into the charts without clearing them (`pushData`
      dedupes by timestamp, so no flicker), and reconnect the WebSocket (cheap: the server sends its cached value at
      once);
    - a past date the user picked → nothing.

    Optional, only if needed: a watchdog for a visible tab that resyncs when no live value arrived for ~2.5 min.

    Not covered: minutes missing in the database itself (a failed collector run) still draw a straight line;
    showing those as breaks (Highcharts `gapSize`) is a separate decision.

    *Status 2026-11-11:* implemented as described, without the watchdog: `KpiDataSourceService` listens for
    `visibilitychange` (resync after 60 s hidden), `KpiFetchServiceService.refreshDataFor` merges the day without
    clearing. Unit tests cover the merge, the short-flick skip, the missed midnight and a picked past day.

    **Resolved 2026-11-11** with frontend 3.13.4 (`3d55016e`, `26230352`; infrastructure `3ba22f0`). Deployed and
    checked by the operator in the browser: back from a background tab, the dashboard fetches the day again and
    reconnects the live data.

47. ~~🟡 **Frontend static files have no deliberate cache headers**~~ (added 2026-11-11)

    `pvlogger-frontend` serves the Angular build from the fat JAR (`staticResources("/", "static")`). The only
    caching rule (`plugins/HTTP.kt`, `CachingHeaders`) gives CSS a 1-day `Expires`; `index.html`, the JS bundles and
    the favicon get no `Cache-Control`, no `ETag` and no `Last-Modified`. So whether a browser picks up a new release
    on a normal reload is left to its heuristics, and nothing can be revalidated cheaply.

    Serving the SPA from the same unit as its backend-for-frontend (`/livedata`) stays as it is: one versioned unit,
    same origin, no CORS in prod, simple ingress allowlist (ADR-0020). Only the headers change.

    To do: the standard SPA split. Files with a content hash in their name (Angular `outputHashing: all`, e.g.
    `main-BHBFA2FT.js`) get `Cache-Control: public, max-age=31536000, immutable`; everything else (`index.html`,
    `favicon.ico`, `3rdpartylicenses.txt`) gets `no-cache` with a strong `ETag`, so a reload revalidates with a
    `304`. Drop the CSS-only `CachingHeaders` rule.

    *Status 2026-11-11:* implemented in frontend 3.13.5. `Routing.kt` `spaResources` sets the headers per file name
    and a SHA-256 `ETag`; `ConditionalHeaders` replaces `CachingHeaders` and answers the `304`. `SpaCachingTest`
    covers both rules and the `304` through the real plugin chain with gzip.

    **Resolved 2026-11-11** with frontend 3.13.5 (`6bf2967b`, `a8141253`; infrastructure `10ee324`). Checked live with
    GET: `/` → `no-cache` + `ETag`, `/main-BHBFA2FT.js` → `public, max-age=31536000, immutable`, `/` with
    `If-None-Match` → `304`. `curl -I` sent HEAD, which the static route did not answer (404, never enabled):
    fixed the same day in frontend 3.13.6 (`enableAutoHeadResponse()` in `spaResources`, regression test in
    `SpaCachingTest`), so `curl -sI https://<pvlogger-host>/` shows the headers too.

48. ~~⚪ **Frontend sends a meaningless `Last-Modified` on its static files**~~ (added 2026-11-11, found while checking
    item 47)

    The fat JAR stores every entry with the same fixed date (1980-02-01, reproducible archives), and Ktor sends it as
    `Last-Modified`. Browsers send `If-None-Match` along with it and the ETag wins, so they are fine. But a client
    that revalidates by date alone gets `304` for a changed `index.html` and keeps the old release. Checked live on
    3.13.6: date only → `304`; outdated ETag + date → `200`.

    Fix: drop the date, the ETag is the only validator worth sending.

    *Status 2026-11-11:* implemented in frontend 3.13.7 (first planned to wait for the next frontend change,
    then released on its own). `plugins/HTTP.kt` `DropLastModified` removes the `LastModifiedVersion` before
    `ConditionalHeaders` turns versions into headers; `SpaCachingTest` checks there is no `Last-Modified` and that
    a date-only check gets `200`.

    **Resolved 2026-11-11** with frontend 3.13.7 (`8408a00a`, `ca5c2f5e`; infrastructure `2e52b62`). Checked live:
    `curl -sI` shows no `last-modified`; date only → `200`, matching ETag → `304`.

49. ~~⚪ **Major/toolchain bumps left over from the 2026-11-12 upgrade run**~~ (added 2026-11-12, then awaiting the owner's
    pick per `docs/UPGRADING.md`; the patch/minor bumps went in on branch `upgrade/2026-11-12-patch-minor`)

    | Bump | From → to | Pros | Cons / breaking | Risk | Effort |
    |---|---|---|---|---|---|
    | Node build image (frontend Dockerfile `web` stage) | `node:24-alpine` → `node:26-alpine` | 26 is the active LTS since 2026-10-28; matches the dev machine (26.13.1); 24 is maintenance-only from now on | Build stage only; the bundle should come out the same. `engines` already says `>=24` | low | small: one `FROM`, image build, `capture.sh` compare, frontend patch bump |
    | `foojay-resolver-convention` (`settings.gradle.kts`) | 0.8.0 → 1.0.0 | Removes the deprecated `JvmVendorSpec.IBM_SEMERU` reference ahead of the next Gradle major; shaded deps | Needs Java 17+ to run Gradle (already 25). Only used to auto-download a JDK, which neither the local build nor the `gradle:9-jdk25` image does — no image content changes | low | tiny: one line, `./gradlew build`, no image bump |
    | TypeScript | 6.0.3 → 7.0.2 | — | **Blocked**: Angular 22.2.6 requires `>=6.0 <6.1`. Wait for an Angular release that supports 7 | — | — |

    Also seen: `@angular/animations` and `@angular/platform-browser-dynamic` are deprecated (npm "latest" points at
    20.x). Removing them is a cleanup, not an upgrade. (The first note here said neither was imported — wrong:
    `@angular/animations` is reached through `@angular/platform-browser/animations`.)

    *Status 2026-11-12:* the 2026-11-12 patch upgrades are live (infrastructure `4b12174`). The owner approved
    Node 26, foojay 1.0.0 and the package removal; implemented on branch `upgrade/2026-11-12-majors`, frontend 3.13.9:
    - foojay 1.0.0: `./gradlew cleanTest build` green, no image change.
    - Packages removed: `provideAnimations()` dropped from `main.ts` (Material 22 animates with CSS and treats no
      animation module like `BrowserAnimations`); the dashboard spec disables animations with Material's
      `MATERIAL_ANIMATIONS` token instead of `NoopAnimationsModule`. JS bundle 1.36 → 1.30 MB. `capture.sh` vs
      the 3.13.8 code: snapshot, screenshots (pixels) and live check identical.
    - Node 26: the Docker `web` stage's `dist` is byte-identical between `node:24-alpine` and `node:26-alpine`;
      `engines` raised to `>=26`. The image starts and serves `/`.

    **Resolved 2026-11-12** with frontend 3.13.9 (`34d62a4b`..`33f11a83`; infrastructure `9218b3e`). Checked live:
    `/` serves the new bundle `main-P436D6YI.js`, `/v1/daydata` → `200`, `/livedata` sends a current KPI frame; the
    owner checked the dashboard in the browser. TypeScript 7 stays blocked until Angular supports it (next upgrade
    run).

50. 🔴 **Charts stay empty when the dashboard loads in a hidden tab** (added 2026-11-13)

    Seen mostly in the iPhone Home Screen app and in Safari, sometimes in Firefox, and in Chrome on a tab restored in
    the background: the KPI tiles show current values, the charts stay empty (no axes, no lines) until a force
    reload. Reproduced in Chrome by opening the dashboard as a real background tab over CDP (Playwright fakes
    `visible`) and switching to it: the charts never filled, not even with the next live values.

    Cause: since frontend 3.13.1 (`d296988c`) `AppBus.kpiValues` emits on animation frames, so a chart component gets
    its first value — and with it its `chartOptions` with series — one frame after `ngOnInit`, and in a hidden tab
    only once it is shown. If Highcharts loaded before that (always in a hidden tab; a race in a visible one, made
    likelier by the cached chunk since item 47), highcharts-angular creates the chart from `{}`. Highcharts'
    `chart.update()` without `oneToOne` updates existing series only and silently ignores the rest, so every later
    update is lost.

    Fix: each chart component builds its full options synchronously in `ngOnInit` (`getChart(Date.now())`) before
    subscribing, so the chart always starts with its series. Each chart spec renders the chart with an `AppBus`
    that never emits (`renderedSeriesInHiddenTab`) and checks the legend shows all series; red before the fix.

    *Status 2026-11-13:* implemented, not released. `ng test` 70/70 green, `ng build` OK; the CDP background-tab
    repro against a local dev server shows axes while hidden and all lines 3 s after switching to the tab.

    *Status 2026-11-13:* live with frontend 3.13.10 (`2cbda2d7`, `9770321c`; infrastructure `6e9ea9e`). `/` serves
    `main-6AKHNOJ4.js`, `/v1/daydata` → `200`. The CDP background-tab repro against the live site, 3 runs: axes while
    hidden, all lines 3 s after switching to the tab (on 3.13.9 they stayed empty for good). Open until the owner
    confirms the iPhone Home Screen app.
