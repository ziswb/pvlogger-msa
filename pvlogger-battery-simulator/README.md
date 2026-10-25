# pvlogger-battery-simulator

Answers the question *"what would a battery storage system actually have done for us?"*
before buying one — by replaying the house's **real, measured energy flows** against a
simulated battery, minute by minute, instead of relying on rules of thumb.

Background and motivation: see `Batteriespeicher_Projekt_Zusammenfassung.md` in the
repository root.

**Status (2026-08-07):** first evaluation concluded — a battery is not economical at
current prices; the purchase is deferred. This tool stays for re-evaluation when
parameters change (energy price, feed-in remuneration, battery cost) — see
`docs/adr/0006-defer-battery-purchase-keep-simulator.md`. Note that it models a flat
tariff only; dynamic-tariff scenarios need an engine extension first.

## How it works

### Input data

The simulator reads the `house_power_kpi_values` table (written every minute by the
data collectors) with a **read-only** database role. Only four columns are used, all
in kW:

| Column | Meaning | Role in the simulation |
|---|---|---|
| `solar_power` | PV production | reporting only |
| `solar_to_house_power` | PV directly consumed | baseline self-consumption |
| `solar_to_grid_power` | measured grid export | **charging budget** |
| `grid_to_house_power` | measured grid import | **discharge demand** |

Using the measured grid flows (instead of recomputing `max(0, solar - house)`) means
the simulation automatically respects the real self-consumption behaviour of the
house. The identity `solar_power = solar_to_house_power + solar_to_grid_power` holds
exactly in the data, which the engine exploits for its bookkeeping.

### Simulation model

For every pair of consecutive samples the engine advances the battery's state of
charge (SoC) over the real interval Δt:

1. **Charge** from PV surplus: AC energy `min(solar_to_grid, max_charge_kw) * Δt` is
   taken from would-be export, multiplied by the charge efficiency, and stored —
   limited by the remaining capacity.
2. **Discharge** to cover demand: AC energy `min(grid_to_house, max_discharge_kw) * Δt`
   is delivered instead of imported, drawn from the cells divided by the discharge
   efficiency — limited by the energy above the reserve floor.

Both steps also accumulate the baseline (import, export, production, consumption), so
"before" and "after" always come from the same pass over the same rows.

### Gap handling

Sampling in the real data is irregular (mostly 1–5 min) and the collectors had
occasional outages. Any interval longer than `-max-gap` (default 5 min) is **excluded**:
the SoC is held, no energy is booked, and the skipped time is reported as
`excluded hours` so you can judge the coverage of a run. Consequence: pick simulation
years by data quality — 2024 has the best coverage (< 1.2 % excluded); winter 2022/23
is daytime-only and unusable for battery analysis.

### What it deliberately does not model

Peak shaving, dynamic tariffs, battery ageing, forecast-based charging, winter
reserve strategies, an EV. The engine is a straightforward greedy
charge-on-surplus / discharge-on-deficit model — which is exactly how a LUNA2000 in
default self-consumption mode behaves.

## Usage

### Prerequisites

- Go (see `go.work` for the toolchain version); run from this directory.
- Network access to the PostgreSQL server (LAN/Tailscale) — which server depends on
  the database move, see [Database connection](#database-connection).
- The password for the `battery_sim_ro` role in `PGPASSWORD`. It lives in 1Password
  (personal account — note the `OP_ACCOUNT` pitfall if your shell defaults to the
  work account):

```bash
export PGPASSWORD="$(op read --account my.1password.com "op://Private/PVLogger battery_sim_ro/password")"
```

### Quick start

```bash
# Compare the two candidate sizes over the best data year
go run ./cmd/simulator -from 2024-01-01 -to 2025-01-01

# Three sizes, with a per-month CSV for plotting
go run ./cmd/simulator -from 2024-01-01 -to 2025-01-01 \
  -battery luna2000-10 -battery luna2000-15 -battery luna2000-20 \
  -csv monthly-2024.csv
```

Or build a binary with `make build` (output in `./build/`).

### Options

| Flag | Default | Description |
|---|---|---|
| `-from` | *(required)* | Start date `YYYY-MM-DD`, Europe/Zurich, inclusive |
| `-to` | *(required)* | End date `YYYY-MM-DD`, Europe/Zurich, exclusive |
| `-battery` | `luna2000-15`, `luna2000-20` | Battery preset; repeat the flag to compare several in one run |
| `-max-gap` | `5m` | Intervals longer than this are excluded (SoC held); Go duration syntax |
| `-import-price` | `0.2576` | Grid import price per kWh (flat tariff) |
| `-feedin-price` | `0.06` | Feed-in remuneration per kWh |
| `-csv` | *(off)* | Write a per-month, per-battery CSV to this path |
| `-summary-csv` | *(off)* | Write a whole-period summary CSV (one row per battery) to this path |
| `-db-host` | `<central-host>` | PostgreSQL host |
| `-db-port` | `5434` | PostgreSQL port |
| `-db-name` | `pvdata` | Database name |
| `-db-user` | `battery_sim_ro` | Database user; password **only** via `PGPASSWORD` |
| `-db-sslmode` | `prefer` | TLS to PostgreSQL: `prefer` falls back to plaintext, `require` does not |

### Database connection

The defaults point at the current server: `<central-host>:5434`, no TLS (the
server refuses it, so `prefer` falls back to plaintext). Keep them until the
database moves to CloudNativePG (infrastructure resilience phase 4; date not set yet).

From the cutover night on, override three flags:

```bash
go run ./cmd/simulator -from 2024-01-01 -to 2025-01-01 \
  -db-host <db-host> -db-port 5432 -db-sslmode require
```

- `<db-host>` (<db-ip>), port 5432.
- `-db-sslmode require` is mandatory: plaintext logins are refused. `require` encrypts
  but does not verify the server certificate.
- Same role `battery_sim_ro` and password; from the LAN it is the only role allowed
  to log in. Read grants are unchanged.
- The connection lands on a read-only standby, which may lag the primary by a few
  milliseconds. Irrelevant for historical replays.
- There is no `kubectl port-forward` path to the new cluster; connect directly from
  the LAN.

The flag defaults switch to the new values in a follow-up change after the cutover.
Details: infrastructure runbook `docs/runbooks/pvlogger-db-to-cnpg.md` (section C,
battery simulator).

### Battery presets

Defined in `internal/sim/battery.go`. All presets use 95 % efficiency per direction
(≈ 90 % round trip), a 5 % reserve SoC, and start empty (at the reserve floor).

| Preset | Capacity | Charge/discharge power | Note |
|---|---|---|---|
| `luna2000-5` | 5 kWh | 2.5 kW | single module |
| `luna2000-10` | 10 kWh | 5 kW | one stack |
| `luna2000-15` | 15 kWh | 5 kW | one stack (favourite) |
| `luna2000-20` | 20 kWh | 10 kW | two stacks, one per SUN2000-8KTL |

To model another system (BYD, Tesla, …), add a preset there — capacity, power limits,
efficiencies, reserve and start SoC are all per-preset parameters.

## Reading the output

```
battery         import kWh  export kWh  self-cons %  autarky %  cycles  RT eff %  saved/y  ΔSoC kWh
LUNA2000-15           3233        7592         43.9       63.4     190      90.2      492      -0.0
```

- **import/export kWh** — remaining grid import and export *with* the battery.
- **self-cons % / autarky %** — self-consumption rate and degree of autarky with the
  battery; the baseline values are printed above the table.
- **cycles** — equivalent full cycles over the period (discharged energy / usable
  capacity). Useful against warranty cycle counts.
- **RT eff %** — realised round-trip efficiency (AC out / AC in); sanity check, should
  sit near the configured 90 %.
- **saved/y** — `avoided_import × import_price − reduced_export × feedin_price` over
  the simulated period.
- **ΔSoC kWh** — SoC difference between end and start. Near zero over a full year;
  a large value would mean the period is skewed (e.g. summer only) and the energy
  balance carries a remainder.

Also check the **coverage line** — if a large share of hours is excluded, pick a
different period (see gap handling above).

Two CSV exports are available, both with explicit `*_no_battery_kwh` and
`*_with_battery_kwh` import/export columns:

- `-csv` — per month and battery: energy figures, self-consumption and autarky with
  the battery, and the excluded hours per month. For plotting seasonal behaviour.
- `-summary-csv` — one row per battery over the whole period: everything the console
  table shows plus the baseline figures, coverage hours, and the tariff prices used.
  For collecting and comparing runs (e.g. different years) in a spreadsheet.

## Development

```bash
make test    # unit tests (synthetic charge/discharge/gap profiles)
make build   # test + darwin/linux binaries into ./build
make lint    # golangci-lint
```

The module is wired into the repo's top-level `Makefile` and `go.work`, so
`./gradlew makeGoBuild` covers it too. There is deliberately no Docker image and no
Kubernetes deployment: this is a local decision-support CLI, not a recurring job.
