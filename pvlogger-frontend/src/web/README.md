# PVLogger Frontend (Angular SPA)

Angular 22 single-page application (Angular Material + Highcharts) showing live and historical
solar KPI data. It is served in production by the Ktor service in this module
(`pvlogger-frontend`, port 8085) — see [CONTEXT.md](../../../CONTEXT.md) for the service map.

## How the SPA reaches production

The SPA is compiled **inside the Docker image build** (`docker/Dockerfile` stage 1: `npm ci`,
Vitest tests, `ng build`) and copied into the Ktor fat JAR's `static/` resources — nothing
built is committed to git. The stage is layered so that when `src/web/` is unchanged
(the usual case), the whole Angular stage — including the tests — is a
Docker layer-cache hit and costs nothing; only the lockfile invalidates `npm ci`, only web
sources invalidate test+build. A test failure fails the image build.

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
npm run build    # Vitest tests first, then ng build into dist/web/
npm run build:ci # the same; what the Docker image build runs
npm test         # Vitest tests only, watch mode
```

56 Vitest tests across components, services, and models; they gate the Docker image build.
They run in Node with jsdom (no browser), via Angular's `@angular/build:unit-test` builder.

- Chart specs use `provideTestHighcharts()` from `src/app/dashboard/charts/testing.ts`:
  highcharts-angular's default loader imports `highcharts/esm/highcharts` without a file
  extension, which Node refuses, and the chart then silently stays empty.
- `expectChartRendered(fixture)` checks that a chart was really drawn.
- jsdom has no text measurement, so read axis labels from the Highcharts axis
  (see `chart-defaults.spec.ts`), not from the DOM.
