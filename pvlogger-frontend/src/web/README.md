# PVLogger Frontend (Angular SPA)

Angular 19 single-page application (Angular Material + Highcharts) showing live and historical
solar KPI data. It is served in production by the Ktor service in this module
(`pvlogger-frontend`, port 8085) — see [CONTEXT.md](../../../CONTEXT.md) for the service map.

## ⚠️ Build output is committed

`ng build` writes directly into `../main/resources/static/` (see `outputPath` in
`angular.json`), and those files are **checked into git**. The Gradle/Docker image build does
not run npm — it packages whatever is committed there into the fat JAR.

So after changing the SPA: run `npm run build` and **commit the regenerated
`src/main/resources/static/` files together with your source change**, otherwise the deployed
image won't contain it.

## Development server

Run the backend(s) locally first, then:

```bash
npm start        # ng serve on http://localhost:4200
```

`ng serve` uses `proxy.conf.json`:

- `/v1` → `http://localhost:8083` (modbus-data-accessor, history queries)
- `/livedata` → `ws://localhost:8085` (the Ktor frontend service, live KPI WebSocket)

## Build & tests

```bash
npm run build    # Karma tests (ChromeHeadless) first, then ng build into ../main/resources/static/
npm test         # Karma tests only, watch mode
```

There is currently only one real test (consolidation-backlog item 22).
