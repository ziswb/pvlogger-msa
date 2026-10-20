# 0020 — Trust boundary: in-cluster calls are trusted, the ingress exposes an allowlist, outside access needs a token

- Status: proposed
- Date: 2026-10-20

## Context

No PVLogger service authenticates anyone: no `ktor-server-auth`, no
`authenticate {}`, no tokens between services (security findings, posture note).
That was never written down, so every review re-derives it (finding 45). It is
also weaker than assumed:

- The **ingress forwarded every `/v1` path** to the data accessor, on Traefik
  (`<pvlogger-host>`) and on the Tailscale ingress alike. The collectors'
  write endpoints live under `/v1`, so any LAN device — the LAN is one flat
  network with guests and IoT devices on it (infrastructure finding 54) — and
  anyone on the tailnet could POST measurement rows. Probed 2026-10-20 with GET:
  `store-kpi-values`, `store-modbus-values` and `store-raw-kpi-register-values`
  answer 405 (route present), an unknown path 404. One bogus lifetime-counter row
  corrupts a day's energy KPIs (ADR-0014) and flows into the dashboard and the
  external export (finding 42). The NetworkPolicies did not help: they allow
  Traefik and Tailscale in by design.
- The frontend's `/metrics` is reachable the same way, under the `/` route.
- Infrastructure ADR-0024 plans the first interface reachable from **outside**
  the home: a KPI export pulled by the external receiver over Tailscale.

The SPA needs exactly three things through the ingress: its static files, the
`/livedata` WebSocket, and `GET /v1/daydata/{date}`.

## Decision

1. **In-cluster calls are trusted.** Service-to-service traffic inside the
   `pvlogger` namespace carries no authentication. The boundary is the
   namespace's default-deny NetworkPolicy plus the ingress allowlist below.
2. **The ingress exposes an allowlist, not prefixes.** Traefik and Tailscale
   route only the SPA's files, `/livedata` and `/v1/daydata` (GET). Write
   endpoints, `/metrics`, the masterdata server and every other data-accessor
   route stay in-cluster. A new browser-facing route is added to the ingress
   explicitly, by its own path; a prefix that also covers internal routes is not
   used.
3. **Browser-facing reads stay unauthenticated.** They serve PV telemetry to the
   home network; the data is not secret, and adding a login to the dashboard is
   not worth its cost.
4. **Anything reachable from outside the home needs a token**, checked by the
   service that serves it. The first case is the ADR-0024 export endpoint (where
   it lives: open question 12). It is the system's first authentication, and it
   stays local to that endpoint — the rule does not spread to in-cluster calls.

## Consequences

- A new mutating endpoint needs **no auth, but must not appear in the ingress**.
  Putting it under `/v1` is fine as long as rule 2 holds — the path prefix is not
  the boundary, the ingress allowlist is.
- Finding 42 is fixed by narrowing the ingress (infrastructure chart), not by
  adding auth to the collectors' write path. Finding 33 (masterdata import) is
  covered by rule 1: the masterdata server has no ingress route.
- `/metrics` must leave the ingress too. On Traefik a rule excludes it; the
  Tailscale ingress matches only path prefixes, so `/` there includes
  `/metrics`. Closing it there needs either a separate management port in
  `pvlogger-shared-ktor` (probes and scraping move with it) or Tailscale traffic
  routed through Traefik's rules. Tracked in finding 18; low severity (JVM and
  HTTP meters only).
- The ingress rules become security-relevant configuration: a change that
  widens them needs the same review as a new endpoint.
- Supersedes nothing. A later decision to add authentication inside the cluster
  would supersede rule 1.
