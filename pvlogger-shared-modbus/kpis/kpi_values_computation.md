# KPI value computation

Identities between the five KPIs (German vocabulary: prod = Producing/Produced,
exp = Exporting/Exported, imp = Importing/Imported, eigv = SelfConsuming/
SelfConsumed, verbr = Consuming/Consumed):

* prod = eigv + exp
* exp = prod - eigv
* eigv = prod - exp
* imp = verbr - eigv
* verbr = eigv + imp

## Live KPIs (per-minute, backlog 33)

Taken from registers wherever a register exists — the SmartDongle's aggregate
block (unit 100, 37498 x20) is ONE coherent snapshot, so no formula can skew:

Measured:
* prod  = register 37516 (total active power, all inverters)
* verbr = register 37500 (load power) — measured DIRECTLY, not computed
* exp / imp = sign split of register 37502 (grid power, flipped to the meter
  convention positive = export)

Computed (no register exists):
* eigv = prod - exp

Callers without a measured consumption source (`KpiLiveData.ConsumingPower ==
nil`, i.e. the full collector until backlog item 34) fall back to the
historical formula:
* verbr = prod - exp + imp

## Today KPIs (daily energy)

Known (register lifetime counters, day-deltas computed by the data accessor,
ADR-0014):
* prod (inverter 32106)
* exp / imp (meter 37119 / 37121)

Computed:
* eigv = prod - exp
* verbr = eigv + imp = prod - exp + imp
