# 0014 — Daily energy KPIs derive from lifetime counters, not device daily registers

- Status: accepted
- Date: 2026-08-13

## Context

Consolidation backlog item 12: the fast collector hard-codes a daily pause
window (00:00, 00:02–00:04) in `main.go` instead of the cron schedule. The
00:00 skip papered over a real defect: the `Produced` KPI came straight from
inverter register 32114 ("daily energy yield"), a counter the device itself
resets at midnight — *shortly after* midnight, as production data shows
(the 00:00:03 sample still carried yesterday's full yield; by 00:01 it read 0).
Every first-sample-of-day therefore recorded yesterday's total (25–44 kWh)
as the new day's production, and since `Consumed = Produced − Exported +
Imported`, the spike propagated. The meter-side KPIs never had this problem:
they read *lifetime* counters (37119/37121) and compute "today" as
`max(today) − max(yesterday)` in the data accessor.

## Decision

**Daily energy KPIs derive from monotonic lifetime counters; device-managed
daily registers are never used as KPI input.** The fast collector reads the
inverter's accumulated yield (register 32106) into
`raw_kpi_register_values.inverter_accumulated_yield` (migration V9), and the
data accessor's energy-today endpoint returns a `producedEnergyToday` delta
computed exactly like the meter deltas. Register 32114 is still collected as
raw reference data and serves as fallback while no lifetime baseline exists
(`producedEnergyToday` is null on the first day after rollout; pre-V9 rows
carry 0, which the delta SQL treats as "no data" via `NULLIF`).

The delta window is anchored on the **newest raw row**, not on `CURRENT_DATE`
at query time: the collector stores its raw values before requesting the
delta, so a collection cycle straddling midnight is attributed to the day its
registers were read instead of yielding a full-day spike through the same
door.

With the midnight sample safe, the fast collector's 00:00 skip is removed.
The remaining 00:02–00:04 skip is unrelated (reserved Modbus window for the
nightly device collectors) and stays until backlog item 12's scheduling half
is resolved.

## Consequences

- The dashboard's midnight production peak is fixed at the source; no
  sampling window needs to dodge the inverter's reset timing anymore.
- `Produced` now updates against the accessor's baseline: correct whenever
  raw values flow, self-healing after collector outages.
- Rollout order matters once: deploy the data accessor (V9 + endpoint) before
  the fast collector. The old accessor would reject nothing (unknown JSON
  fields are ignored), but the new collector expects the extended response.
- One day of `producedEnergyToday = null` after rollout (fallback to 32114,
  i.e. exactly the previous behaviour), correct deltas from day two.
- If a second inverter is ever added, summing lifetime counters stays correct
  only while the device set is stable; a device joining or leaving breaks the
  summed baseline for one day.
- **The full collector still violates this decision**: it computes `Produced`
  from the *latest value* of register 32114 (`InverterEnergyToday` in
  `data-accessor-service.go`), masked only by its `5-59/2` schedule never
  sampling 00:00–00:04 — and by being suspended (infrastructure backlog 2).
  Before the fast/full toggle is ever used again, align it: route `Produced`
  through the existing "kW today so far" delta endpoint with register 32106
  (the mechanism its meter KPIs already use), and port the newest-row anchor
  fix to `RegisterReader`'s copy of the delta SQL (see backlog item 20's
  duplication note).
