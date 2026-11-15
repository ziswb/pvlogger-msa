# pvlogger-modbus-mock

The local stand-in for the PV plant (ADR-0025): a deterministic synthetic plant
that a Modbus mock server serves to the real collectors, and that a backfill job
writes as history, so the Tilt environment has a plausible dashboard from the
first start. Local development only, never production.

## The synthetic plant (`internal/plant`)

Every value is a function of the minute: no random generator, so the mock server
and the backfill agree on every snapshot. Physical units; the Modbus register
mapping (gains, sign conventions) lives with the mock server.

| Part | Model |
|---|---|
| Site | Latitude 47°, longitude 10° E (generic Central Europe, configurable) |
| Plant | 10 kWp, one inverter, panels 30° tilted to the south, performance ratio 0.85, no battery |
| PV | Clear-sky output from the sun's elevation and azimuth, attenuated by the air mass, times a sky factor |
| Weather | Each local day sunny, mixed or overcast (more overcast in winter); mixed days have clouds in 10-minute blocks |
| House load | ~230 W base, morning, midday and evening peaks, two appliance blocks a day (1.5–2.5 kW, 20–60 min); about 4,500 kWh a year |
| Grid | Import = load the PV cannot cover, export = PV the load does not use |
| Counters | Lifetime yield, daily yield (resets at local midnight), exported and imported energy: sums of the per-minute values since the origin (2025-01-01) |

Days are local days in the configured zone, so the daily yield resets at the
same midnight the data accessor uses, including the 23- and 25-hour days of a
clock change.

## The mock server (`cmd/mock-server`, `internal/mock`)

Serves the plant over Modbus TCP in place of the SmartDongle; the device, fast
and full collectors run against it unchanged. It answers at the current minute:

| Unit | Registers |
|---|---|
| Dongle (`DONGLE_DEVICE_ID`, default 100) | Aggregate block 37498–37516: input, load, grid (positive = import), battery (0), total active power. Active + grid = load exactly, as on the real dongle |
| Every other unit (inverter, meter behind it) | Identity 30000–30073 (model `SUN2000-10KTL-M1`, fake serial `SIM0000001`, rated power), PV strings, phase voltages and currents, active power, status (on-grid / standby), counters 32106 and 32114; meter 37100–37138 (positive = export) with counters 37119 and 37121 |
| Read device identification (0x2B) | Basic objects (vendor, product, version) and the device list (one inverter) |

Registers it does not model read as 0, so the full collector's catalog sweep
always gets a complete answer. Other function codes get exception 0x01.

| Variable | Default | |
|---|---|---|
| `MOCK_PORT` | `8502` | Unprivileged; the chart's Service exposes it as 502 |
| `DONGLE_DEVICE_ID` | `100` | Same setting as the collectors |
| `TZ` | container zone | The plant's local time (day curve, daily-yield reset) |

## The history backfill (`cmd/backfill`, `internal/backfill`)

Writes the plant's last days into the data accessor, so the dashboard has a
history on first start. For every minute it stores the raw KPI row the fast
collector would have stored and the KPI row the shared aggregation would have
computed (shared `kpis` code; today's energy as the counters' difference to
local midnight), both dated that minute. Mock server and backfill report the
same reading at register resolution (`mock.ReadingOf`), so the switch from
history to live data is seamless.

The data accessor must run with `DEV_ACCEPT_CLIENT_TIMESTAMPS=true` (chart:
`dataAccessor.devAcceptClientTimestamps`), otherwise the raw rows are dated on
arrival. A day that already has KPI rows is filled only before its first row:
a rerun stores nothing, and the history ends where live data begins.

| Variable | Default | |
|---|---|---|
| `BACKFILL_DAYS` | `30` | Counted back from today's local midnight |
| `DA_HOST`, `DA_PORT`, `DA_PROTOCOL` | `localhost`, `8083`, `http` | The data accessor, as for the collectors |
| `TZ` | container zone | Same zone as the mock server and the data accessor |

One image holds both commands: the entrypoint is `/mock-server`, a Job runs
`/backfill`.

```bash
make test lint build      # build/<command> (linux) and build/<command>-darwin
```
