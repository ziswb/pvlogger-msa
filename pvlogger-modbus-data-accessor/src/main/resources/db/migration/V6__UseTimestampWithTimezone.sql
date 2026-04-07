-- Convert TIMESTAMP columns to TIMESTAMPTZ so the database explicitly tracks timezone info.
-- Existing values are treated as UTC (matching how the application stores them).

SET timezone = 'UTC';

ALTER TABLE house_power_kpi_values
    ALTER COLUMN date_created TYPE TIMESTAMPTZ USING date_created AT TIME ZONE 'UTC';

ALTER TABLE modbus_response_data
    ALTER COLUMN date_created TYPE TIMESTAMPTZ USING date_created AT TIME ZONE 'UTC',
    ALTER COLUMN request_time TYPE TIMESTAMPTZ USING request_time AT TIME ZONE 'UTC',
    ALTER COLUMN response_time TYPE TIMESTAMPTZ USING response_time AT TIME ZONE 'UTC';

ALTER TABLE raw_kpi_register_values
    ALTER COLUMN date_created TYPE TIMESTAMPTZ USING date_created AT TIME ZONE 'UTC';
