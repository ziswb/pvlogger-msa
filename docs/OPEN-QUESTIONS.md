# Open Questions — pvlogger-msa

Things that could **not** be derived from the repository (2026-08-06 review). Answer
inline, then promote stable answers to CONTEXT.md or an ADR and delete the question.

## Strategy

1. **Collector end state** (most consequential): `pv-fast-data-collector` (0.x,
   5 hard-coded registers, currently the one running) and `pv-full-data-collector`
   (0.x, all dynamic registers, currently suspended — partly by a chart bug) look
   like the successor pair to `pv-device-data-collector` (3.x, still deployed for
   nightly static data). What is the intended end state? Which module can be
   deprecated, and should `pv-full`'s duplicated code be extracted or deleted?
2. **Battery decision**: the battery simulator (newest module, active until
   2026-07) exists to evaluate a LUNA2000 purchase. Has the decision been made?
   If yes, does the simulator stay (e.g. for tariff studies per the
   Zusammenfassung's "Langzeit-Erweiterungen") or get archived?
3. **Why microservices at all** for a single-site homelab app — resume-driven,
   fault isolation, independent deploys? Worth one honest paragraph in an ADR;
   it sets the bar for when a new service is justified vs. a new job in an
   existing one.

## Architecture details

4. **`fsdata` schema ownership**: fusionsolar writes it via sqlc but ships no
   migrations — the DDL lives only in the running DB and old dumps. Intentional
   (schema managed manually) or a gap to fix with Flyway/tern?
5. ~~**Who consumes the kpi-exporter output** (`pv_*` JSON on OneDrive, every
   5 min)?~~ Answered 2026-08-07: the operator's receiver — he runs a small
   platform comparing the energy/PV data of three family households (operator,
   another household, his own). Promoted to CONTEXT.md component table; format and
   cadence are a de-facto external contract.
6. **Mock server in production?** The Helm chart ships a template + pinned tag
   for it. Dev-only or actually deployed?
7. **`Category.DEVLST` and `Type.MLD`** exist in the model; `MLD` throws
   `NotImplementedError` in the data accessor. Planned work or dead vocabulary?
8. **Local dev loop**: is Tilt still the intended flow (Tiltfile references dead
   paths), or has "build → push → helm upgrade" replaced it?

## Site / hardware (partially in the Batteriespeicher doc — confirm)

9. ~~Which physical device answers Modbus at `<dongle-ip>` (SmartDongle?), and
   which physical units are Modbus IDs meter `1`, inverters `1,16`?~~
   Answered 2026-08-07: `.224` is the SmartDongle, plugged into inverter `1`;
   IDs `1` and `16` are the two SUN2000-8KTL-M1 inverters (16 RS485
   daisy-chained to 1). The meter at ID `1` is a virtual device backed by the
   DTSU666-H wired to inverter `1`. Promoted to CONTEXT.md (Domain
   vocabulary → Device).
10. ~~The site coordinates hard-coded in sunrise-sunset (<site-lat>…, <site-lon>…) — correct
    and intentionally public in the repo?~~
    Answered 2026-08-07: correct — they are the operator's home coordinates.
    But hard-coding them is not intentional design: moving them to
    configuration is now CONSOLIDATION-BACKLOG item 27.
