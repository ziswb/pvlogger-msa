-- Corrective migration: the Go data collectors previously added a hardcoded +1 hour
-- to time.Now() before storing epoch millis. V6 migrated these values to TIMESTAMPTZ
-- treating them as UTC, but they were actually UTC+1. Subtract 1 hour to fix.
--
-- Only rows created BEFORE the fixed image (0.8.0) was deployed are affected.
-- Helm revision 74 deployed at 2026-04-07 08:40:00 UTC; first correct data
-- arrived shortly after. We use 08:40 UTC as the cutoff.
--
-- Updates are batched (10 000 rows at a time) to limit WAL pressure and lock duration.
-- Indexes on updated columns are dropped beforehand and recreated after.

-- Drop indexes on columns we are about to bulk-update.
DROP INDEX IF EXISTS house_power_kpi_values_date_created_idx;
DROP INDEX IF EXISTS modbus_response_data_date_created_idx;
DROP INDEX IF EXISTS modbus_response_data_request_time_idx;
DROP INDEX IF EXISTS modbus_response_data_response_time_idx;
DROP INDEX IF EXISTS raw_kpi_register_values_date_created_idx;

-- Batch-update house_power_kpi_values
DO $$
DECLARE
    batch_size CONSTANT INT := 10000;
    rows_updated INT;
BEGIN
    LOOP
        UPDATE house_power_kpi_values
        SET date_created = date_created - INTERVAL '1 hour'
        WHERE id IN (
            SELECT id FROM house_power_kpi_values
            WHERE date_created < TIMESTAMPTZ '2026-04-07T08:40:00Z'
            LIMIT batch_size
        );
        GET DIAGNOSTICS rows_updated = ROW_COUNT;
        EXIT WHEN rows_updated = 0;
        RAISE NOTICE 'house_power_kpi_values: updated % rows', rows_updated;
    END LOOP;
END $$;

-- Batch-update modbus_response_data
DO $$
DECLARE
    batch_size CONSTANT INT := 10000;
    rows_updated INT;
BEGIN
    LOOP
        UPDATE modbus_response_data
        SET date_created = date_created - INTERVAL '1 hour',
            request_time = request_time - INTERVAL '1 hour',
            response_time = response_time - INTERVAL '1 hour'
        WHERE id IN (
            SELECT id FROM modbus_response_data
            WHERE date_created < TIMESTAMPTZ '2026-04-07T08:40:00Z'
            LIMIT batch_size
        );
        GET DIAGNOSTICS rows_updated = ROW_COUNT;
        EXIT WHEN rows_updated = 0;
        RAISE NOTICE 'modbus_response_data: updated % rows', rows_updated;
    END LOOP;
END $$;

-- Batch-update raw_kpi_register_values
DO $$
DECLARE
    batch_size CONSTANT INT := 10000;
    rows_updated INT;
BEGIN
    LOOP
        UPDATE raw_kpi_register_values
        SET date_created = date_created - INTERVAL '1 hour'
        WHERE id IN (
            SELECT id FROM raw_kpi_register_values
            WHERE date_created < TIMESTAMPTZ '2026-04-07T08:40:00Z'
            LIMIT batch_size
        );
        GET DIAGNOSTICS rows_updated = ROW_COUNT;
        EXIT WHEN rows_updated = 0;
        RAISE NOTICE 'raw_kpi_register_values: updated % rows', rows_updated;
    END LOOP;
END $$;

-- Recreate all dropped indexes.
CREATE INDEX house_power_kpi_values_date_created_idx ON house_power_kpi_values (date_created);
CREATE INDEX modbus_response_data_date_created_idx ON modbus_response_data (date_created);
CREATE INDEX modbus_response_data_request_time_idx ON modbus_response_data (request_time);
CREATE INDEX modbus_response_data_response_time_idx ON modbus_response_data (response_time);
CREATE INDEX raw_kpi_register_values_date_created_idx ON raw_kpi_register_values (date_created);
