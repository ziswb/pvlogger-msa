# 0023 — Database roles live in CloudNativePG; migrations only GRANT

- Status: accepted (2026-10-26)
- Date: 2026-10-26

## Context

Since the cutover on 2026-10-25, the PVLogger databases run on the in-cluster
CloudNativePG cluster `pvlogger-db` (infrastructure ADR-0022). CloudNativePG can
manage roles declaratively through `spec.managed.roles` and reconciles them
continuously.

Before the move, roles came from two places. The app role `pvlogger` was set up
by hand from a runbook (backlog item 40). `battery_sim_ro` was created by a
Flyway migration (`V8__AddBatterySimReadOnlyRole.sql`), with its login and
password added manually afterwards. A migration that creates a role on a
cluster where the operator also manages roles would leave the two fighting over
it. It would also mean no single place lists which roles exist.

The infrastructure side set the rule on 2026-10-26.

## Decision

1. **New database roles are created on the infrastructure side first**, in the
   CloudNativePG cluster's `spec.managed.roles` (infrastructure repo, under
   `roles/pvlogger_db`), including login and password handling.
2. **Flyway migrations here (and tern migrations in fusionsolar, ADR-0022) never
   run `CREATE ROLE`, `ALTER ROLE` or `DROP ROLE`.** They only `GRANT` (and
   `REVOKE`) privileges to roles that already exist.
3. **V8 stays untouched.** It is in the Flyway history and will not run again.
   Applied migrations are never edited.

## Consequences

- A feature that needs a new role ships in two steps: first the role on the
  infrastructure side, then the migration with its GRANTs. The migration fails
  if the role is missing, so the order is visible.
- Tests on Testcontainers (ADR-0009) have no CloudNativePG. A test whose
  migration grants to a new role must create that role in its own setup first.
- Role passwords no longer go through manual `ALTER ROLE` scripts in this repo.
