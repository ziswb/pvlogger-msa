---- tern: disable-tx ----
-- Index every foreign key column that references fs_response (backlog 66).
--
-- All six are ON DELETE RESTRICT and none had an index, so deleting from
-- fs_response sequentially scanned every referencing table once per deleted row:
-- the one-off cleanup of backlog 66 took hours on its last statement.
--
-- The operator created these indexes by hand on the production database
-- before this migration existed, with the same names: IF NOT EXISTS makes it a
-- no-op there. CONCURRENTLY keeps the tables writable while the indexes are
-- built, so this migration runs without a transaction (the disable-tx line
-- above; tern then runs one statement at a time).
--
-- If a build fails, PostgreSQL leaves an INVALID index behind and IF NOT EXISTS
-- would skip it on a rerun: drop it (`\d <table>` shows INVALID), then rerun.

create index concurrently if not exists fs_device_real_kpi_response
    on fs_device_real_kpi (response);

create index concurrently if not exists fs_station_real_kpi_response
    on fs_station_real_kpi (response);

create index concurrently if not exists fs_response_params_basic_response
    on fs_response_params_basic (response);

create index concurrently if not exists fs_response_params_device_response
    on fs_response_params_device (response);

create index concurrently if not exists fs_response_params_extended_response
    on fs_response_params_extended (response);

create index concurrently if not exists fs_response_to_station_extended_data_response_id
    on fs_response_to_station_extended_data (response_id);
