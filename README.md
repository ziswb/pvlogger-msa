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

> Status: personal project, running in production on a home k3s cluster. This README
> is a skeleton; architecture diagram, screenshots and a "try it" section follow.

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

See [CONTEXT.md](CONTEXT.md) for the domain vocabulary and data flow, and
[docs/adr/](docs/adr/) for the decisions behind it.

## Build

```bash
./gradlew build         # JVM services (tests need Docker for Testcontainers)
./gradlew makeGoBuild   # Go services
```

## Public code, private configuration

This repository contains no site configuration. Hosts, network details, site
identifiers and credentials live in a separate private infrastructure repository and
appear here only as placeholders such as `<db-host>` or `<tailnet>`.

## License

Apache License 2.0, see [LICENSE](LICENSE) and [NOTICE](NOTICE).

Not affiliated with or endorsed by Huawei. Huawei, FusionSolar and SUN2000 are
trademarks of Huawei Technologies Co., Ltd.
