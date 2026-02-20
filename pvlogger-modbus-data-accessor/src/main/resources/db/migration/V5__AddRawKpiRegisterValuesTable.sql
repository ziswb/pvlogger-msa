CREATE TABLE raw_kpi_register_values
(
    id                    SERIAL PRIMARY KEY,
    date_created          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    job_id                TEXT             DEFAULT 'NO-JOB-ID-GIVEN' NOT NULL,
    inverter_active_power DOUBLE PRECISION DEFAULT 0.0 NOT NULL,
    meter_active_power    DOUBLE PRECISION DEFAULT 0.0 NOT NULL,
    inverter_daily_yield  DOUBLE PRECISION DEFAULT 0.0 NOT NULL,
    meter_exported_energy DOUBLE PRECISION DEFAULT 0.0 NOT NULL,
    meter_imported_energy DOUBLE PRECISION DEFAULT 0.0 NOT NULL
);

CREATE INDEX raw_kpi_register_values_date_created_idx ON raw_kpi_register_values (date_created);
