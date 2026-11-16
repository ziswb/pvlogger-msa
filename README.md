# PVLogger

Monitoring and data collection for a home photovoltaic installation (Huawei SUN2000
inverters), built as a small polyglot microservice system on Kubernetes.

- **Collects** inverter and meter registers over Modbus TCP every minute and derives
  energy-flow KPIs (solar to house, grid import/export, self-consumption).
- **Integrates** the Huawei FusionSolar Northbound API as a second, cloud-side source.
- **Serves** an Angular dashboard with live data over WebSocket and daily history.
- **Exports** the latest KPIs to a third party through a token-protected endpoint.
- **Simulates** a home battery against the measured history before buying one
  ([battery simulator](pvlogger-battery-simulator/README.md)).

> Status: personal project, running in production on a home k3s cluster. An
> architecture diagram and screenshots will follow.

## Architecture

Kotlin/Ktor services hold the data and serve the UI; Go CronJobs do the collecting.

| Component | Language | Role |
|---|---|---|
| `pv-fast-data-collector` | Go | per-minute Modbus poll of the KPI registers |
| `pv-full-data-collector` | Go | hourly sweep of all registers in the master data |
| `pv-device-data-collector` | Go | nightly static/device-identifier registers |
| `pvlogger-modbus-data-accessor` | Kotlin | stores and queries register values and KPIs |
| `pvlogger-masterdata-server` | Kotlin | Modbus register master data |
| `pvlogger-frontend` | Kotlin + Angular | dashboard SPA and `/livedata` WebSocket |
| `pvlogger-fusionsolar`, `-auth` | Go | FusionSolar API integration |
| `pvlogger-kpi-exporter` | Go | token-protected KPI export |
| `pvlogger-modbus-mock` | Go | local development only: a synthetic plant over Modbus TCP and its history backfill |

Everything deploys with one Helm chart, [`deploy/helm/pvlogger`](deploy/helm/pvlogger/README.md),
which pins every image version.

See [CONTEXT.md](CONTEXT.md) for the domain vocabulary and data flow, and
[docs/adr/](docs/adr/) for the decisions behind it.

## Build

```bash
./gradlew build         # JVM services (tests need Docker for Testcontainers)
./gradlew makeGoBuild   # Go services
```

## Try it locally

With [Rancher Desktop](https://rancherdesktop.io/) (Kubernetes and Traefik on) and
[Tilt](https://tilt.dev/):

```bash
tilt up --context rancher-desktop
```

This builds every image, installs the chart with a dev database and a synthetic PV
plant in place of the real inverter, writes 30 days of history, and serves the
dashboard on `http://localhost/`. It works on arm64 and amd64 hosts.

## Contributing

Enable the leak-guard hooks once per clone (this repository is public; see
[`.githooks/README.md`](.githooks/README.md)):

```bash
git config core.hooksPath .githooks
```

Security issues: see [SECURITY.md](SECURITY.md).

## Public code, private configuration

This repository contains no site configuration. Hosts, network details, site
identifiers and credentials live in a separate private infrastructure repository and
appear here only as placeholders such as `<db-host>` or `<tailnet>`.

## License

Apache License 2.0, see [LICENSE](LICENSE) and [NOTICE](NOTICE).

Not affiliated with or endorsed by Huawei. Huawei, FusionSolar and SUN2000 are
trademarks of Huawei Technologies Co., Ltd.
