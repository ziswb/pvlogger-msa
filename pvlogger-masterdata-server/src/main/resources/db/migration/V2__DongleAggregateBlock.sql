-- SmartDongle aggregate block (its own logical device, unit 100) — ADR-0016,
-- backlog items 33 + 35. Register names/types follow the
-- wlcrs/huawei-solar-lib register table; the block is absent from Huawei's
-- inverter interface PDF and CSV exports, so these project-curated rows enter
-- the catalog via migration instead of the CSV importer (ADR-0017).
--
-- Contract notes:
--  * The `type` values must match pvlogger-shared-modbus/registers — the
--    shared KPI aggregation reads 37500/37502/37516 back from the data
--    accessor's per-type value tables (a drift test in that Go package
--    parses this file and fails on divergence).
--  * The undocumented 37506–37515 filler row keeps 37498..37517 one
--    consecutive run, so the collector merges the block into a SINGLE Modbus
--    request = one coherent snapshot (the whole point of the block, see
--    backlog 33). Do not remove it or the sweep splits into two requests.
--  * 37516 is catalogued as the device serves it (W, gain 1); the Go
--    definitions deliberately carry kW/THOUSAND there to match the raw-row
--    column convention — documented in register-definitions.go.
insert into modbus_register_master_data
    (device_type, no, name, read_write, type, unit, gain, address, quantity, scope, category)
values ('DONGLE', 1, 'Total input power', 'RO', 'U32', 'W', 'ONE', 37498, 2, '', 'DYNAMIC'),
       ('DONGLE', 2, 'Load power', 'RO', 'U32', 'W', 'ONE', 37500, 2, '', 'DYNAMIC'),
       ('DONGLE', 3, 'Grid power', 'RO', 'I32', 'W', 'ONE', 37502, 2, '', 'DYNAMIC'),
       ('DONGLE', 4, 'Battery power', 'RO', 'I32', 'W', 'ONE', 37504, 2, '', 'DYNAMIC'),
       ('DONGLE', 5, 'Reserved', 'RO', 'MLD', 'NA', 'ONE', 37506, 10, '', 'DYNAMIC'),
       ('DONGLE', 6, 'Total active power', 'RO', 'I32', 'W', 'ONE', 37516, 2, '', 'DYNAMIC')
on conflict (address) do nothing;
