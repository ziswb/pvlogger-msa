-- Corrective migration: the Go data collectors previously added a hardcoded +1 hour
-- to time.Now() before storing epoch millis. V6 migrated these values to TIMESTAMPTZ
-- treating them as UTC, but they were actually UTC+1. Subtract 1 hour to fix.
--
-- Only rows created BEFORE the fixed image (0.8.0) was deployed are affected.
-- Helm revision 74 deployed at 2026-04-07 08:40:00 UTC; first correct data
-- arrived shortly after. We use 08:40 UTC as the cutoff.

UPDATE house_power_kpi_values
SET date_created = date_created - INTERVAL '1 hour'
WHERE date_created < TIMESTAMPTZ '2026-04-07T08:40:00Z';

UPDATE modbus_response_data
SET date_created = date_created - INTERVAL '1 hour',
    request_time = request_time - INTERVAL '1 hour',
    response_time = response_time - INTERVAL '1 hour'
WHERE date_created < TIMESTAMPTZ '2026-04-07T08:40:00Z';

UPDATE raw_kpi_register_values
SET date_created = date_created - INTERVAL '1 hour'
WHERE date_created < TIMESTAMPTZ '2026-04-07T08:40:00Z';
