# 0015 — Modbus access windows live in cron schedules, not in code

- Status: accepted
- Date: 2026-08-13

## Context

The scheduling half of consolidation backlog item 12: the fast collector
skipped 00:02–00:04 in `main.go` so the three nightly device collectors
(00:02/00:03/00:04) get the Modbus interface to themselves. A live
concurrency test against the SmartDongle (2026-08-13) settled how the device
actually behaves: it is **last-connect-wins** — a new client's TCP connect
succeeds and kills the *established* connection (EOF on the old socket);
nothing is refused or queued. Consequences observed:

- The fast collector (connects last, runs ~2 s) always wins a collision; the
  ~45 s device jobs are the victims.
- Connect-retry is the wrong remedy — connects never fail. And
  `ExecuteRequest`'s three read attempts are useless against a killed socket
  (all EOF within 1 ms, no reconnect between attempts).
- Kubernetes `backoffLimit` retry recovers a killed device job systematically
  (the respawn lands in the ~56 s gap between fast runs), but each collision
  leaves a failed pod, which fires `PvloggerCronJobFailed` — the alert counts
  failed pods even when the job eventually succeeds.

So the reserved window is still the right mechanism, and it protects the
*device jobs from the fast collector*, not the other way around.

## Decision

**Access windows for the shared Modbus endpoint are expressed in CronJob
schedules; collector code contains no time logic.** A single cron expression
cannot say "every minute except 00:02–00:04", so the fast collector is two
CronJobs sharing one image and config:

- `pv-fast-data-collector-cronjob` — `* 1-23 * * *`
- `pv-fast-data-collector-cronjob-hour0` — `0-1,5-59 0 * * *`

Monitoring treats the pair as one logical job: a dedicated
`PvloggerFastCollectorStale` rule alerts when the *fresher* of the two
`last_successful_time`s exceeds the frequent-tier threshold (the individual
CronJobs are excluded from the generic frequent tier, since each is stale for
part of every day by design — the caveat ADR-0013 anticipated). The Grafana
"Time since last successful run" panel merges the pair via `label_replace`.

## Consequences

- `main.go` shrinks to dispatch-only; the schedule is fully visible in
  `kubectl get cronjobs` and the Helm chart (infrastructure repo).
- The device collectors keep `backoffLimit` as a safety net against
  accidental overlap (manual runs, schedule drift) — accepted with the known
  cost of a failed-pod alert when it triggers.
- Reconnect-between-attempts in `TcpHandler` remains an optional hardening,
  with a caveat from the test: two reconnecting clients ping-pong the line on
  a last-connect-wins device until the shorter job finishes.
- All CronJobs set `spec.timeZone` explicitly (Europe/Zurich), so the hour-0
  boundary is deliberate rather than inherited from the node timezone.
