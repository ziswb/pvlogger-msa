# 0006 — Defer the battery purchase; keep the simulator for re-evaluation

- Status: accepted (recorded 2026-08-07 from the operator's answer to open question 2)
- Date: 2026-08-07

## Context

`pvlogger-battery-simulator` was built to evaluate a LUNA2000 purchase by
replaying the house's real measured energy flows (see its README and
`Batteriespeicher_Projekt_Zusammenfassung.md`). The 2026 evaluation concluded:
**at current parameters a battery is not economical.**

## Decision

- **No battery purchase for now.**
- **The simulator stays** — it is the standing tool to re-run the evaluation
  when parameters change, e.g. energy prices rise sharply, feed-in
  remuneration drops toward zero, or battery prices halve.
- Reusability for those scenarios is confirmed by design: import and feed-in
  prices are CLI flags (no code change for `-feedin-price 0`); battery systems
  are per-preset parameters, other vendors addable; and it reads the live
  `pvdata` database for any `-from`/`-to` range, so future years with fresh
  collector data are directly simulable. Battery *price* is not a simulator
  input — economics are `saved/y` (simulator output) vs. purchase price,
  compared outside the tool.

## Consequences

- The module remains in the build (`go.work`, `makeGoBuild`), and its
  read-only DB role (`battery_sim_ro`) and 1Password entry stay provisioned.
- Known limitation for future scenarios: the engine models a **flat tariff
  only**. If re-evaluation involves dynamic/hourly pricing, the engine needs
  an extension first (deliberately unmodeled today, per its README).
