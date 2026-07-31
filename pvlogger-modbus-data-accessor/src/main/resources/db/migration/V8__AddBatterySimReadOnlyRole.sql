-- Read-only role for the battery simulator (analysis of historical KPI data).
--
-- The role is created WITHOUT login capability and WITHOUT a password here,
-- because this file is committed to git and also runs against H2 in tests.
-- LOGIN and the password (stored in 1Password, item "PVLogger battery_sim_ro")
-- are set manually once via scripts/set-battery-sim-ro-password.sh:
--
--     ALTER ROLE battery_sim_ro LOGIN PASSWORD '<from 1Password>';
--
-- Note: roles are cluster-wide in PostgreSQL. This migration fails if the
-- role already exists; it must not be created manually beforehand.
CREATE ROLE battery_sim_ro;

GRANT SELECT ON house_power_kpi_values TO battery_sim_ro;
GRANT SELECT ON raw_kpi_register_values TO battery_sim_ro;
