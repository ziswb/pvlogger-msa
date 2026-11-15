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
