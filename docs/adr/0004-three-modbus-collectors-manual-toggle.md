# 0004 — Three Modbus collectors: fast by default, full as manual alternative, device nightly

- Status: accepted (recorded 2026-08-07 from the operator's answer to open question 1)
- Date: 2026-08-07

## Context

The site's Modbus TCP endpoint (the SmartDongle, see CONTEXT.md → Device)
allows **only one client at a time**. Historically a single collector gathered
*all* dynamic registers, which requires fetching register definitions from the
masterdata service first — an API call whose latency works against a
per-minute polling schedule.

## Decision

- **`pv-fast-data-collector` is the default dynamic collector.** It hard-codes
  the five KPI registers *deliberately*: no masterdata dependency, no API-call
  latency, maximum collection speed.
- **`pv-full-data-collector` stays** as the alternative for when all dynamic
  registers should be collected (masterdata-driven). It is **not** a successor
  or predecessor of the fast collector — the two are permanent alternatives.
- **Exactly one of the two dynamic collectors is enabled at a time** (Modbus
  one-client constraint). The Helm toggle exists to switch between them and is
  **operated manually**; there is no automated switching.
- **`pv-device-data-collector` stays permanently** for nightly static/config
  register collection. Its nightly schedule is deliberately placed to avoid
  colliding with the per-minute dynamic polling.

## Consequences

- Neither dynamic collector gets deprecated; `pv-full-data-collector`'s
  duplicated code should be **extracted into the shared Go libraries, not
  deleted** (consolidation backlog items 6–7).
- The chart bug that keeps the full collector permanently suspended
  (infrastructure backlog #2) defeats the intended manual toggle and should be
  fixed.
- The one-client constraint is enforced only by operator discipline (the
  manual toggle) plus schedule placement — nothing technically prevents
  enabling both dynamic collectors.
