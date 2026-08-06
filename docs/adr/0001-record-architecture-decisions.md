# 0001 — Record architecture decisions

- Status: accepted
- Date: 2026-08-06

## Context

PVLogger grew organically from a monolith (`pvLogger-classic`) into polyglot
microservices. Structural decisions live only in code, so parallel
implementations of the same concern have accumulated (three env-config helpers,
two persistence clients, duplicated collector code — see the consolidation
backlog).

## Decision

Record architecture decisions as short ADRs in `docs/adr/`. Retroactive ADRs
document current state and mark unknown rationale with ⚠️ GAP instead of
inventing it.

## Consequences

Consolidation outcomes ("we now always do X") get a durable home; humans and AI
check here before introducing a new pattern.
