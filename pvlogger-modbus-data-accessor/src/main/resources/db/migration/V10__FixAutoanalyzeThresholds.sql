-- Autoanalyze can never fire on the big append-mostly fact tables, so the query
-- planner works from statistics that silently rot for months.
--
-- Observed 2026-08-26: `read-latest-register-value` for the dongle aggregate
-- block took 29.6 s server-side and raced the fast collector's 30 s HTTP
-- timeout. Column statistics were last collected 2026-04-28; the dongle block
-- (logic_device_id = 100, registers 37502/37516) only went live 2026-08-14, so
-- the MCV list for modbus_response_data.logic_device_id was still {1,16} with
-- frequencies summing to 1.0. The planner therefore estimated
-- `logic_device_id = 100` at 1 row (actual: 16 557) and `register_address =
-- 37516` likewise, and built a 1-row x 1-row nested loop that really ran
-- 16 557 x 16 557 = 274 M iterations. Same query after correct estimates: 6.8 ms.
--
-- Root cause is the *scale factor*: autovacuum_analyze_scale_factor = 0.05 on a
-- 34 M-row table demands ~1.7 M modifications, while these tables grow by a few
-- thousand rows a day — the next autoanalyze was ~11 months out, and none had
-- run since April. Scale-factor thresholds assume a table's churn is
-- proportional to its size; for append-only fact tables it is proportional to
-- *time*, so a flat threshold is the right model.
--
-- 5 000 rows puts every hot table on a 1-3 day cadence. ANALYZE samples
-- 300 x default_statistics_target = 30 000 rows regardless of table size, so the
-- cost of running it that often is negligible.
--
-- modbus_characteristic_curve_data and modbus_register_string_values are
-- deliberately left alone: they are small enough that the 0.05 scale factor
-- already yields a *tighter* threshold than 5 000 would.

ALTER TABLE modbus_response_data
    SET (autovacuum_analyze_scale_factor = 0, autovacuum_analyze_threshold = 5000);
ALTER TABLE modbus_register_integer_values
    SET (autovacuum_analyze_scale_factor = 0, autovacuum_analyze_threshold = 5000);
ALTER TABLE modbus_register_uinteger_values
    SET (autovacuum_analyze_scale_factor = 0, autovacuum_analyze_threshold = 5000);
ALTER TABLE modbus_register_short_values
    SET (autovacuum_analyze_scale_factor = 0, autovacuum_analyze_threshold = 5000);
ALTER TABLE modbus_register_ushort_values
    SET (autovacuum_analyze_scale_factor = 0, autovacuum_analyze_threshold = 5000);
ALTER TABLE house_power_kpi_values
    SET (autovacuum_analyze_scale_factor = 0, autovacuum_analyze_threshold = 5000);
ALTER TABLE raw_kpi_register_values
    SET (autovacuum_analyze_scale_factor = 0, autovacuum_analyze_threshold = 5000);

-- The settings above only govern *future* autoanalyze runs; they do not repair
-- the stale statistics that are already in the catalog. Do that once here, so a
-- deployment of this migration is sufficient to fix a rotted database and no
-- manual step is needed. ANALYZE is transaction-safe (unlike VACUUM), so it is
-- fine inside Flyway's migration transaction. On a fresh/empty database these
-- are no-ops.
ANALYZE modbus_response_data;
ANALYZE modbus_register_integer_values;
ANALYZE modbus_register_uinteger_values;
ANALYZE modbus_register_short_values;
ANALYZE modbus_register_ushort_values;
ANALYZE house_power_kpi_values;
ANALYZE raw_kpi_register_values;
