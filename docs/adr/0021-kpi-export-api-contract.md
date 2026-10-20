# 0021 — KPI export: the cluster serves the finished file, a relay on the receiver's server writes it

- Status: accepted (2026-10-20)
- Date: 2026-10-20

## Context

Infrastructure **ADR-0024** (amended 2026-10-20) replaces the OneDrive path of
the KPI export: a container from this repo runs on the receiver's server
and reaches PVLogger over Tailscale. The flow, the network path, the tailnet
policy and the token Secret are decided there. This ADR records the part this
repo owns — where the endpoint lives and what crosses the wire — and answers
open question 12.

What the receiver agreed to (2026-10-20): his platform calls the container over
HTTP; on each call the container fetches the latest state, writes today's file
(same JSON, same name pattern `<prefix>-YYYY-MM-DD_HH-MM-SS.mmm.json`) into a
directory on his server, cleans up files older than 48 hours as the CronJob does
today, and returns the JSON. Only the latest state matters; no catch-up.

## Decision

1. **One binary, three modes** (`KPI_EXPORTER_MODE`) in `pvlogger-kpi-exporter`,
   the module that already owns the format — no new service (ADR-0007):
   `export` (default: today's CronJob), `serve` (cluster endpoint), `relay`
   (container on his server).
2. **The cluster serves the finished file.** `GET /v1/export/latest` returns the
   export JSON byte for byte as the CronJob writes it, and the file name in
   `Content-Disposition: attachment; filename="…"`. The relay writes body and
   name as received. Format, file prefix and formulas therefore live only in the
   cluster; changing them never requires touching his server.
3. **Token, per ADR-0020 rule 4.** `Authorization: Bearer <token>`, compared in
   constant time; 401 otherwise. The token is read from a file
   (`KPI_EXPORT_TOKEN_FILE`) on every request, so a rotated Secret applies
   without a restart; a missing or empty file rejects everything. `GET /healthz`
   needs no token and is not routed by the export ingress.
4. **The relay joins the operator's tailnet itself** (Tailscale's `tsnet`,
   ADR-0024 option A): no Tailscale install or account on his side; its state
   dir keeps the node identity, so the auth key matters only on first start.
5. **The relay trusts nothing it receives blindly**: file name must be a plain
   `*.json` name (no paths, not hidden), body must be valid JSON under 1 MiB,
   written via temp file + rename; any failure answers 502 and writes nothing.
   Its `/export` is unauthenticated — it is his network and his decision whom to
   let call it.

## Consequences

- `pv_verb_akt_w` changed meaning with this release: it was export + import,
  now the measured consumption (`ownConsumption`, register 37500 — what the
  dashboard shows). The receiver is told.
- The exporter image is multi-arch (linux/arm64 for the cluster, linux/amd64 for
  his server) and carries `tsnet` (~30 MB binary). The repo moved to Go 1.27.3
  for it.
- The CronJob mode stays until the receiver has switched over; then the chart
  drops it and the OneDrive volume (infrastructure side).
- The endpoint is the system's first authenticated interface. Widening what it
  returns is an API change: the relay writes whatever the cluster sends.
