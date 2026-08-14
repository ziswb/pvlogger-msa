# 0016 — One KPI path: fast collects per-minute, full sweeps hourly in a uniform window

- Status: accepted (recorded 2026-08-14 from the operator's decision)
- Date: 2026-08-14
- Supersedes: ADR-0004 entirely; ADR-0015's two-CronJob fast split and pair
  monitoring (the windows-live-in-cron-schedules principle stands).

## Context

ADR-0004 kept fast and full as equivalent, manually-toggled alternatives. That
equivalence decayed invisibly *because* full never runs: it still violates
ADR-0014 (`Produced` from register 32114's latest value), unnoticed for months,
and the toggle has been fiction since the chart bug suspended full
(infrastructure backlog 2). Backlog item 33 widens the gap: live KPIs will come
from the SmartDongle's own aggregate block (unit 100 — one coherent 20-register
snapshot), which full cannot read without a new masterdata device type.
Meanwhile ADR-0015's two-CronJob fast pair produced monitoring complexity on
day one (pair-merge dashboard defect, alert-tier special-casing) — the hour-0
special case was accidental complexity. The operator keeps full deliberately:
for its educational value and for the masterdata-driven extended-sweep
capability (hypothetical use, valued anyway). Full's last working cadence
(every 2 minutes) shows one sweep fits a 3-minute window with headroom.

## Decision

- **Minutes 02–04 of every hour are the reserved non-fast Modbus window**
  (ADR-0015's mechanism, made uniform). Fast becomes a **single CronJob**
  again: `0-1,5-59 * * * *`.
- Hour 0's window stays with the nightly device collectors. **Hours 1–23
  belong to full: an hourly sweep** (`2 1-23 * * *`), `activeDeadlineSeconds`
  bounding it inside the window (the dongle is last-connect-wins; an overrun
  must fail cleanly, not fight fast's reconnect).
- **No toggle, no fallback role.** Fast is the sole per-minute KPI collector,
  permanently, with its deliberately hard-coded register set (ADR-0004's speed
  rationale survives). Full is the masterdata-driven extended sweep (all
  DYNAMIC+ALARM registers) — which requires a **dongle device type (unit 100)
  in masterdata** and the device model; until then it sweeps today's catalog.
- **KPI computation exists exactly once**: an `aggregate(jobId)` component in
  `pvlogger-shared-modbus` that reads the run's raw rows back from the data
  accessor and derives + stores the KPIs. Both collectors invoke it in-process
  after POSTing raw values — the path is identical, not merely shared. Fast's
  private KPI computation and full's private aggregator path are **deleted,
  not aligned**. Daily KPIs remain the accessor's day-delta SQL (ADR-0014).
- **Hard invariant: aggregation is per `jobId`** — one collection run, one
  coherent snapshot. Never "newest value per register": that would mix
  interleaved fast/full runs and reintroduce the sampling skew backlog 33
  exists to remove.

## Consequences

- KPI parity between collectors is structural, not maintained; the "align full
  before the toggle returns" debt (ADR-0014 consequences, backlog 12 tail) is
  resolved by deletion.
- ADR-0015's pair machinery is obsolete: `PvloggerFastCollectorStale` and the
  dashboard pair-merge can be retired; the single fast CronJob rejoins the
  generic frequent alert tier, and full joins it too (hourly cadence).
- Fast skips minutes 02–04 every hour (3 of 60 samples). Dashboards bridge via
  Prometheus lookback; latest-KPI consumers (frontend `/livedata`,
  kpi-exporter) may serve values up to ~4 minutes old once an hour — accepted.
  A full run inside the window emits a KPI row through the same aggregate
  path, partially filling the gap.
- The full chain (masterdata fetch → sweep → raw POST → aggregate) is
  exercised 23×/day — no "suspended until needed" rot — and extended register
  data actually accumulates (incl. battery power 37504, relevant to the
  ADR-0006 re-evaluation).
- Prerequisites for full's hourly schedule: the chart bug (infrastructure
  backlog 2). The masterdata dongle type gates only the unit-100 registers,
  not the schedule.
