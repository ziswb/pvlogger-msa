# PVLogger — Context

This document describes PVLogger at the **stable** level: purpose, domain language,
component responsibilities, and data flow. Volatile detail (versions, build
commands, layouts) lives in [CLAUDE.md](CLAUDE.md). Decisions are in
[docs/adr/](docs/adr/), known inconsistencies in
[docs/CONSOLIDATION-BACKLOG.md](docs/CONSOLIDATION-BACKLOG.md), and undocumented
knowledge in [docs/OPEN-QUESTIONS.md](docs/OPEN-QUESTIONS.md). How this repo relates
to the `infrastructure` repo and the legacy folders:
`infrastructure/docs/LANDSCAPE.md`.

> Marker convention: `⚠️ GAP` = knowledge only the operator has. Answer it, move
> the answer here or into an ADR, delete the marker.

## Purpose

PVLogger monitors a private **16 kWp photovoltaic installation** (2× Huawei
SUN2000-8KTL-M1 inverters, Huawei DTSU666-H smart meter, Modbus TCP via a
SmartDongle — see `Batteriespeicher_Projekt_Zusammenfassung.md` for the site
description, in German). It:

- polls the inverters/meter over **Modbus TCP** (once per minute),
- pulls portal data from the **Huawei FusionSolar OpenAPI**,
- stores everything in PostgreSQL,
- computes household energy **KPIs**,
- serves a **dashboard** (live via WebSocket + per-day history),
- exports KPI snapshots as JSON files (OneDrive-synced),
- and feeds a **battery simulator** used to evaluate a LUNA2000 battery purchase
  (2026 verdict: not economical yet — purchase deferred, simulator kept for
  re-evaluation when prices change; ADR-0006).

It runs as microservices on the homelab K3S cluster; deployment is owned by the
`infrastructure` repo (Helm chart under `ansible/roles/pvlogger_helm_deploy/`).
The microservice architecture was learning-driven; new services must justify
themselves pragmatically — default to a job/endpoint in an existing service
(ADR-0007).

## Domain vocabulary

- **Device** — a Modbus logic device addressed by unit ID. Exactly two types:
  `METER` and `INVERTER`. Current site: meter ID `1`, inverter IDs `1,16`.
  Physical topology behind the IDs: the **SmartDongle** (`<dongle-ip>`) is
  the Modbus TCP gateway, physically plugged into inverter `1`; inverter `16`
  (the second SUN2000-8KTL-M1) is RS485 daisy-chained to inverter `1` and
  reached through the same dongle. The meter at ID `1` is a **virtual
  device**: the DTSU666-H is wired to inverter `1` via RS485 and has no unit
  ID of its own — its readings are exposed in the meter register block of
  unit `1`, so meter and inverter `1` share a unit ID and are distinguished
  only by register addresses.
- **Register** — a Modbus holding register described by a *register definition*:
  address, quantity, type (U16/U32/I16/I32/STR/…), gain, unit, and a **category**:
  `STATIC` (nameplate data), `DYNAMIC` (measurements), `CONFIG`, `ALARM`, `DEVLST`.
  `Category.DEVLST` and `Type.MLD` are currently unhandled **by design** — the
  `NotImplementedError` in the data accessor is a deliberate fail-fast guard so a
  future feature that receives such data cannot silently skip it. `DEVLST` is
  loosely planned for implementation one day (confirmed 2026-08-07).
- **Master data** — the register definitions themselves: Huawei's imported
  from three SUN2000 CSV files, project-curated ones (the DONGLE block) added
  by Flyway data migration (ADR-0017); served over REST.
- **Raw KPI register values** — the per-minute raw snapshot row (one per fast
  run): inverter active power, meter active power, inverter daily yield,
  inverter accumulated (lifetime) yield, meter exported energy, meter imported
  energy. Its lifetime counters feed the accessor's day-delta endpoint
  (ADR-0014); the live KPI values are read back per run from the dongle block
  in the generic register store (ADR-0016).
- **KPI** — derived household energy figures. Two naming vocabularies exist and
  map 1:1 (the mapping is documented in `PvLoggerKpiData.kt`):

  | Compute/wire name (Go) | Storage/REST name | German (export/docs) |
  |---|---|---|
  | producing / produced | solar_power / daily_energy_yield | prod |
  | exporting / exported | solar_to_grid_power / daily_energy_exported | exp / verk |
  | importing / imported | grid_to_house_power / daily_energy_imported | imp / kauf |
  | consuming / consumed | own_consumption / daily_energy_consumed | verbr / verb |
  | selfConsuming / selfConsumed | solar_to_house_power / daily_energy_self_covered | eigv |

  Derivations (`pvlogger-shared-modbus/kpis/`): `consuming = producing − exporting
  + importing`; `selfConsuming = producing − exporting`.
- **Live data** — the latest KPI row, pushed to the browser over the `/livedata`
  WebSocket (refreshed every 60 s).

## Component map

### Kotlin/Ktor HTTP services (long-running)

| Service | Responsibility |
|---|---|
| `pvlogger-modbus-data-accessor` | **The central data service.** Owns the `pvdata` DB (register values, raw KPI registers, house KPIs). All collectors write through it; frontend and exporter read from it. REST paths are defined in `pvlogger-shared-rest`. |
| `pvlogger-masterdata-server` | Owns the `pvmasterdata` DB. Imports Huawei's register definitions from CSVs; project-curated definitions (the DONGLE unit-100 block) ship as Flyway data migrations instead (ADR-0017). Serves the catalog to collectors and the mock server. |
| `pvlogger-frontend` | Serves the prebuilt Angular SPA, proxies the latest KPI as the `/livedata` WebSocket. History data goes **directly** SPA → data accessor (via ingress path `/v1`). |
| `pvlogger-sunrise-sunset` | **Deprecated** (ADR-0005). Fetches and caches sunrise/sunset times for the site coordinates (own `sunrisesunset` DB). Historically gated FusionSolar collection to daylight hours; nothing calls it anymore — retirement is backlog item 28. |
| `pvlogger-modbus-mock-server` | Emulates a Modbus TCP device (raw TCP, not HTTP) using real register definitions from the masterdata server. **Local development only — never deployed to production** (confirmed 2026-08-07); the Helm chart's mock-server template exists solely for local use. |

### Go CronJob collectors (short-lived batch runs)

| Service | Responsibility |
|---|---|
| `pv-fast-data-collector` | Every minute except 00:02–00:04 (two cron schedules — the device collectors' reserved Modbus window, ADR-0015): reads the dongle's aggregate live-power block (unit 100, one coherent request — backlog 33) plus the energy counters, stores the block in the generic register store and the raw KPI row, then invokes the **shared `aggregate(jobId)` path** (`pvlogger-shared-modbus/aggregate`, ADR-0016/backlog 34) which derives and stores the KPIs (daily production as a lifetime-counter delta, ADR-0014). **The sole per-minute KPI collector** — registers are hard-coded *deliberately* for maximum speed (no masterdata dependency/API call; the set lives in `pvlogger-shared-modbus/registers`). Still pending from ADR-0016: back to a **single CronJob** (`0-1,5-59 * * * *` — minutes 02–04 of *every* hour become the reserved non-fast window, backlog 36). |
| `pv-full-data-collector` | Reads *all* DYNAMIC+ALARM registers (definitions from masterdata) — including the dongle's unit-100 block as one consecutive Modbus request (backlog 35, migration V2 + DONGLE device type) — then invokes the same shared `aggregate(jobId)` path as fast; its private aggregator was deleted (ADR-0016/backlog 34). **Currently permanently suspended by a chart bug** (see infrastructure backlog #2). Target per ADR-0016 (supersedes the ADR-0004 toggle): the masterdata-driven **extended sweep**, running **hourly in the 02–04 window of hours 1–23** (backlog 36), no KPI role of its own. Deploy masterdata-server (applies V2) before enabling it, or the aggregation step fails for lack of the block. |
| `pv-device-data-collector` | Nightly: reads STATIC/CONFIG registers, device identifiers and device list. Stays permanently; scheduled at night to avoid the per-minute dynamic polling (one-Modbus-client constraint, ADR-0004). |
| `pvlogger-fusionsolar` | Six scheduled jobs pulling FusionSolar OpenAPI KPIs (station/device, real-time/day/month/year) — writes **directly** to the `fsdata` DB (not via the data accessor). |
| `pvlogger-fusionsolar-auth` | Refreshes the FusionSolar API token into a K8s secret every 20 min; the collector jobs read it. |
| `pvlogger-kpi-exporter` | Every 5 min: reads the latest KPI and writes a German-keyed (`pv_*`) JSON file to an NFS/OneDrive-synced volume. Consumed by the operator's receiver: he runs a small platform comparing the energy/PV data of three family households (operator, another household, his own). This is an **external consumer** — treat the file naming, keys, and 5-min cadence as a de-facto contract. |

### Shared libraries

Kotlin: `pvlogger-shared-model` (Modbus domain model), `pvlogger-shared-rest`
(REST path constants + KPI DTO), `pvlogger-shared-database` (Hikari + Flyway
bootstrap). Go: `pv-logger` (logging, env config), `pvlogger-shared-modbus`
(hand-rolled Modbus TCP client, KPI math, masterdata/persistence REST clients,
register/device-identifier collector jobs, Pushgateway metrics, the hard-coded
KPI register set in `registers`, and the single KPI aggregation path in
`aggregate` — ADR-0016).

### Tools (not deployed)

`pvlogger-battery-simulator` — local CLI replaying historical KPI data against
simulated LUNA2000 batteries (deliberately no Docker/K8s; see its README).
Purchase deferred as uneconomical (2026); the CLI stays as the standing
re-evaluation tool (ADR-0006).
`pvlogger-database/` — local Docker Compose Postgres + historical migration
runbooks; not a service.

## Data flow

```mermaid
flowchart LR
    MB[Inverters + Meter\nModbus TCP] -->|poll| FAST[fast collector]
    MB -->|poll| FULL[full collector*]
    MB -->|nightly| DEV[device collector]
    MD[masterdata-server] -->|register defs| FULL & DEV & MOCK[mock server]
    FAST & FULL & DEV -->|REST| DA[modbus-data-accessor]
    FS[FusionSolar API] --> FSC[fusionsolar jobs] -->|SQL| FSDB[(fsdata)]
    DA --> PVDB[(pvdata)]
    MD --> MDDB[(pvmasterdata)]
    DA -->|read-latest-kpi| FE[frontend Ktor] -->|WebSocket /livedata| SPA[Angular SPA]
    DA -->|/v1/daydata via ingress| SPA
    DA -->|read-latest-kpi| EXP[kpi-exporter] --> ONE[OneDrive files\nfamily PV-comparison platform]
```

\* full collector currently suspended (chart bug).

Rule: **all Modbus-derived data flows through the data accessor's REST API** —
collectors never touch `pvdata` directly. The FusionSolar pipeline is the
exception: it writes its own `fsdata` DB directly. ⚠️ GAP: deliberate exception
or drift? (`fsdata`'s missing migrations, by contrast, are explained: knowingly
dropped in the Kotlin→Go rewrite — see ADR-0002 — and to be reintroduced per
backlog item 29.)

## Databases

One PostgreSQL server (the homelab central server) hosts four databases. Each is
owned by exactly one service, which also owns its Flyway migrations:

| DB | Owner | Content |
|---|---|---|
| `pvdata` | modbus-data-accessor | register values, raw KPI registers, house KPIs |
| `pvmasterdata` | masterdata-server | register definitions |
| `sunrisesunset` | sunrise-sunset | cached sun times — deprecated, drop with the module (ADR-0005) |
| `fsdata` | fusionsolar (no migrations — known gap, backlog item 29) | FusionSolar responses & KPIs |

All four are dumped nightly by the db-backup CronJob (infrastructure repo).

## Build, versioning & deployment (summary)

- Images are built by `scripts/build.sh` and pushed to the private registry
  `<registry-host>:5000`. **A service's version is the `LABEL version` in its
  Dockerfile** ([ADR-0003](docs/adr/0003-image-versioning-and-deployment.md)).
- **Deployed** versions are pinned in the Helm chart's `values.yaml` in the
  infrastructure repo (declared single source of truth for what runs).
- Cron schedules and runtime config (Modbus host `<dongle-ip>`, device IDs,
  DB endpoints) live in the chart/Ansible role, not here.
- Local dev: `tilt up` per CLAUDE.md. Tilt (including the mock server) is the
  **intended environment for developing larger changes** (confirmed
  2026-08-07) — but it has rotted: the Tiltfile references a
  `pvlogger-infrastructure/` directory that no longer exists and has no
  `docker_build` steps, and the mock server is outdated. Repair is backlog
  item 23 (fix, don't remove), the mock-server refresh is item 30 — both low
  priority; verify before relying on the flow.
