# 0025 — One synthetic plant in Go feeds the mock server and the history backfill

- Status: accepted (2026-11-15); implemented with backlog items 23 and 30
- Date: 2026-11-15

## Context

The local Tilt environment (backlog 23) is the dev loop for larger changes and
the "try it" path of the public repo; its screenshots must never show real data.
The Kotlin mock server answered every Modbus request with random values per
register type: no day curve, counters that jump around, KPIs that do not add up.
The dashboard's history also needs data on first start, about 30 days of it.

KPIs are derived in Go (`pvlogger-shared-modbus`): live values from the dongle
block, daily energy as day-differences of lifetime counters, computed by the data
accessor relative to the current date. The data accessor timestamps raw KPI
rows itself; KPI rows already carry the collector's `dateCreated`.

## Decision

1. **One deterministic synthetic plant, in Go** (generic location and size, see
   the module README). Every value is a function of time; noise and weather are
   derived from the minute and the date, not from a random generator, and the
   counters are sums of per-minute values from a fixed origin. So any process
   that asks for time *t* gets the same snapshot.
2. **A Go mock server replaces the Kotlin one.** It serves the plant over Modbus
   TCP, reusing the register definitions and framing of `pvlogger-shared-modbus`;
   the real collectors run against it unchanged. This extends ADR-0002 (Go for
   batch jobs) to the device emulator: it shares the Modbus code that lives in Go.
3. **A Go backfill job writes the history.** For every past minute it stores the
   plant's raw KPI register row and the KPI row, computed with the shared `kpis`
   code; daily energy comes from the plant's own counters, not from the data
   accessor's day-difference SQL (which only computes for today and is exercised
   by the live path every minute).
4. **The raw-row store accepts a client `dateCreated` only in a dev build of the
   data accessor.** Without it a raw row that carries the field is refused (400),
   not stored as of now: *amended 2026-11-15 (backlog 57)*, it was ignored at
   first, so a backfill against a stock data accessor silently dated a month of
   raw rows today. With raw rows from the backfill, today's day-difference has its
   midnight baseline and the switch from backfill to live data is seamless.

   *Amended 2026-11-17 (backlog 60):* the switch is fixed when the image is
   built, not set at deploy time. The Dockerfile's build arg
   `DEV_ACCEPT_CLIENT_TIMESTAMPS` (default `false`) writes
   `/opt/pvlogger/image.conf`, which `application-prod.conf` includes last; the
   profile has no environment override for it. Only the Tiltfile passes `true`;
   CI, releases and `scripts/build.sh` build the default. This replaces the
   chart value `dataAccessor.devAcceptClientTimestamps`, which became an env var
   and needed a render guard (only with the mock server) and a CI check of that
   guard: the safety sat in configuration, and the data accessor could not tell
   Tilt from production, both run `application-prod.conf`. Now a released image
   cannot accept the timestamps whatever the values say. The local
   `application-dev.conf` (a Gradle run, never in an image) turns it on.

## Consequences

- Live (mock + real collectors + `aggregate` + day-difference SQL) and history
  (backfill) come from one model, so the dashboard looks continuous and the KPI
  identities hold everywhere.
- The Kotlin mock server and its masterdata-driven random values go away.
- Production behaviour is unchanged. The data accessor Tilt runs differs from
  the release image by this one build arg, on purpose.
- Someone who can edit the Deployment can still mount their own
  `image.conf` or run a dev image; that is the same trust as the KPI store
  below, not a new hole.
- The KPI store already trusts a client `dateCreated`: any in-cluster caller can
  write KPI rows into the past. That is within ADR-0020's trust model (in-cluster
  calls are trusted) and not new.
