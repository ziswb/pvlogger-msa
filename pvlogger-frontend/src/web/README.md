# PVLogger Frontend (Angular SPA)

Angular 19 single-page application (Angular Material + Highcharts) showing live and historical
solar KPI data. It is served in production by the Ktor service in this module
(`pvlogger-frontend`, port 8085) — see [CONTEXT.md](../../../CONTEXT.md) for the service map.

## How the SPA reaches production

The SPA is compiled **inside the Docker image build** (`docker/Dockerfile` stage 1: `npm ci`,
Karma tests, `ng build`) and copied into the Ktor fat JAR's `static/` resources — nothing
built is committed to git. The stage is layered so that when `src/web/` is unchanged
(the usual case), the whole Angular stage — including the Chromium install and tests — is a
Docker layer-cache hit and costs nothing; only the lockfile invalidates `npm ci`, only web
sources invalidate test+build. A Karma test failure fails the image build.

Consequence for local runs: a locally started Ktor frontend serves no SPA (there is no
`src/main/resources/static/` in the tree) — use `ng serve` for the UI and the local Ktor
process only for `/livedata`.

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
npm run build    # Karma tests (ChromeHeadless) first, then ng build into dist/web/
npm run build:ci # same, but ChromeHeadlessNoSandbox — what the Docker image build runs
npm test         # Karma tests only, watch mode
```

47 Karma specs across components, services, and models; they gate the Docker image build.
