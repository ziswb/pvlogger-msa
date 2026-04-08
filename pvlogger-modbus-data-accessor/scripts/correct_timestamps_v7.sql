-- Manual timestamp correction script for V7 migration.
-- Targets house_power_kpi_values, modbus_response_data, and raw_kpi_register_values.
--
-- Run via psql (must NOT be run inside a transaction block):
--   psql <connection-string> -f correct_timestamps_v7.sql
--
-- The script is resumable: progress is tracked in _v7_correction_progress.
-- Restarting after an interruption will skip already-committed days.
-- After a successful full run, inspect _v7_correction_progress and then drop it.
--
-- Background: Go collectors stored epoch-millis as UTC+1 instead of UTC.
-- V6 migrated these to TIMESTAMPTZ treating them as UTC, so all historical
-- timestamps are 1 hour too late. Only rows before the fixed image (0.8.0,
-- Helm revision 74, 2026-04-07 08:40 UTC) are affected.

-- ---------------------------------------------------------------------------
-- 1. Tracking table (idempotent)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS _v7_correction_progress (
    table_name   TEXT NOT NULL,
    day_processed DATE NOT NULL,
    rows_updated  INT  NOT NULL,
    processed_at  TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (table_name, day_processed)
);

-- ---------------------------------------------------------------------------
-- 2. Correction procedure  (requires PostgreSQL 11+ for COMMIT in procedures)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE correct_timestamps_v7()
LANGUAGE plpgsql AS $$
DECLARE
    cutoff         CONSTANT TIMESTAMPTZ := TIMESTAMPTZ '2026-04-07T08:40:00Z';
    current_day    DATE;
    day_window_end TIMESTAMPTZ;
    rows_updated   INT;
BEGIN
    -- Find the oldest calendar day that still has uncorrected data across
    -- both target tables. We anchor on rows that are still within [day,
    -- day+1) in the tables as they exist right now — already-shifted rows
    -- are tracked and will be skipped below.
    SELECT MIN(d)::DATE INTO current_day
    FROM (
        SELECT date_trunc('day', MIN(date_created)) AS d
          FROM house_power_kpi_values
         WHERE date_created < cutoff
        UNION ALL
        SELECT date_trunc('day', MIN(date_created)) AS d
          FROM modbus_response_data
         WHERE date_created < cutoff
        UNION ALL
        SELECT date_trunc('day', MIN(date_created)) AS d
          FROM raw_kpi_register_values
         WHERE date_created < cutoff
    ) sub
    WHERE d IS NOT NULL;

    IF current_day IS NULL THEN
        RAISE NOTICE 'No rows found before the cutoff — nothing to do.';
        RETURN;
    END IF;

    RAISE NOTICE 'Starting from day %. Cutoff: %.', current_day, cutoff;

    WHILE current_day::TIMESTAMPTZ < cutoff LOOP
        -- Upper bound for this day's window (clipped to cutoff on the last day).
        day_window_end := LEAST((current_day + 1)::TIMESTAMPTZ, cutoff);

        -- ----------------------------------------------------------------
        -- house_power_kpi_values
        -- ----------------------------------------------------------------
        IF NOT EXISTS (
            SELECT 1 FROM _v7_correction_progress
             WHERE table_name = 'house_power_kpi_values'
               AND day_processed = current_day
        ) THEN
            UPDATE house_power_kpi_values
               SET date_created = date_created - INTERVAL '1 hour'
             WHERE date_created >= current_day::TIMESTAMPTZ
               AND date_created <  day_window_end;
            GET DIAGNOSTICS rows_updated = ROW_COUNT;

            INSERT INTO _v7_correction_progress (table_name, day_processed, rows_updated)
            VALUES ('house_power_kpi_values', current_day, rows_updated);

            RAISE NOTICE '[%] house_power_kpi_values: % rows updated', current_day, rows_updated;
        ELSE
            RAISE NOTICE '[%] house_power_kpi_values: skipped (already committed)', current_day;
        END IF;

        -- ----------------------------------------------------------------
        -- modbus_response_data
        -- ----------------------------------------------------------------
        IF NOT EXISTS (
            SELECT 1 FROM _v7_correction_progress
             WHERE table_name = 'modbus_response_data'
               AND day_processed = current_day
        ) THEN
            UPDATE modbus_response_data
               SET date_created  = date_created  - INTERVAL '1 hour',
                   request_time  = request_time  - INTERVAL '1 hour',
                   response_time = response_time - INTERVAL '1 hour'
             WHERE date_created >= current_day::TIMESTAMPTZ
               AND date_created <  day_window_end;
            GET DIAGNOSTICS rows_updated = ROW_COUNT;

            INSERT INTO _v7_correction_progress (table_name, day_processed, rows_updated)
            VALUES ('modbus_response_data', current_day, rows_updated);

            RAISE NOTICE '[%] modbus_response_data: % rows updated', current_day, rows_updated;
        ELSE
            RAISE NOTICE '[%] modbus_response_data: skipped (already committed)', current_day;
        END IF;

        -- ----------------------------------------------------------------
        -- raw_kpi_register_values
        -- ----------------------------------------------------------------
        IF NOT EXISTS (
            SELECT 1 FROM _v7_correction_progress
             WHERE table_name = 'raw_kpi_register_values'
               AND day_processed = current_day
        ) THEN
            UPDATE raw_kpi_register_values
               SET date_created = date_created - INTERVAL '1 hour'
             WHERE date_created >= current_day::TIMESTAMPTZ
               AND date_created <  day_window_end;
            GET DIAGNOSTICS rows_updated = ROW_COUNT;

            INSERT INTO _v7_correction_progress (table_name, day_processed, rows_updated)
            VALUES ('raw_kpi_register_values', current_day, rows_updated);

            RAISE NOTICE '[%] raw_kpi_register_values: % rows updated', current_day, rows_updated;
        ELSE
            RAISE NOTICE '[%] raw_kpi_register_values: skipped (already committed)', current_day;
        END IF;

        COMMIT;
        RAISE NOTICE '[%] committed.', current_day;

        current_day := current_day + 1;
    END LOOP;

    RAISE NOTICE 'Correction complete.';
END;
$$;

-- ---------------------------------------------------------------------------
-- 3. Run
-- ---------------------------------------------------------------------------
CALL correct_timestamps_v7();

-- ---------------------------------------------------------------------------
-- 4. Cleanup  (run manually after verifying the data looks correct)
-- ---------------------------------------------------------------------------
-- DROP PROCEDURE correct_timestamps_v7();
-- DROP TABLE _v7_correction_progress;
