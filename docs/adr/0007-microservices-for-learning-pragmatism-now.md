# 0007 — Microservices were chosen for learning; the bar for new services is pragmatic now

- Status: accepted (recorded 2026-08-07 from the operator's answer to open question 3)
- Date: 2026-08-07

## Context

PVLogger is a single-site homelab application, yet it is built as polyglot
microservices on Kubernetes (see ADR-0002). For a system of this size that is
not the default choice, and the reason was never written down.

## Decision

The honest rationale: the architecture was **learning-driven**. The operator
works as a Dev(Sec)Ops engineer and wanted hands-on experience with Kubernetes
and modern architectural patterns — not fault isolation, scale, or independent
deploys. That goal was achieved, and the perspective has since shifted:
**effectiveness and economy now outweigh architectural ambition.**

Consequences for new work — the bar going forward:

- The existing service split stays; it works and is not worth consolidating
  for its own sake (known duplication is tracked in the consolidation
  backlog).
- A new capability defaults to a **job in an existing Go service** (the
  job-dispatch pattern) or an **endpoint in an existing Kotlin service**.
- A **new microservice needs a concrete, pragmatic justification** — learning
  value alone no longer counts.

## Consequences

- Reviewers (human or AI) should push back on new-service proposals that an
  existing service could absorb.
- Effectiveness/economy is also the yardstick for infrastructure choices
  around the app, in line with the retirement-style decisions already recorded
  (e.g. ADR-0005).
