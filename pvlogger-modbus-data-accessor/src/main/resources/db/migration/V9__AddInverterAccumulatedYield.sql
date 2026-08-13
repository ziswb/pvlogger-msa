-- Lifetime (monotonic) inverter yield counter, register 32106. Daily production
-- is derived as a day-delta of this column (like meter import/export) instead of
-- trusting the device's self-resetting daily register 32114 (ADR-0014).
-- Default 0.0 marks pre-migration rows as "no data" for the delta computation.
ALTER TABLE raw_kpi_register_values
    ADD COLUMN inverter_accumulated_yield DOUBLE PRECISION NOT NULL DEFAULT 0.0;
