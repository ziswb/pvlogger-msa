-- Drop the three single-column indexes that V11's composites make redundant
-- (backlog 41). A btree on (a, b) serves every lookup a btree on (a) serves,
-- so these cost write amplification and ~511 MB of disk for nothing.
--
-- V11 held them back until the composites were seen winning in production.
-- Checked on .2 2026-10-20 (PostgreSQL 18.2):
--
--   index                                         size    idx_scan
--   modbus_response_data_job_dev_idx       (V11)  237 MB   103 896
--   modbus_register_uinteger_value_idx     (V11)  338 MB    34 632
--   modbus_register_integer_value_idx      (V4)   737 MB    69 264
--   modbus_response_data_job_id_idx               180 MB         0  <- dropped
--   modbus_register_integer_values_reg_addr_idx   227 MB         0  <- dropped
--   modbus_register_uinteger_values_reg_adr_idx   104 MB         0  <- dropped
--
-- Both read-latest-register-value plans (I32 37516, U32 37500) are a nested
-- loop of two Index Conds on the composites: 0.11 ms / 16 buffers and
-- 0.07 ms / 9 buffers, against 0.99 ms / 38 and 2.63 ms / 293 before V11.
--
-- NOT dropped here: modbus_response_data_dev_id_idx (124 MB, also 0 scans).
-- No composite leads with logic_device_id, so it is not redundant by
-- construction; dropping it would rest on usage counters alone, and how far
-- back those reach is unknown (stats_reset is empty). Same for the three
-- timestamp indexes (backlog 41).
--
-- Unlike V11's CREATE INDEX, no manual step: DROP INDEX only changes the
-- catalog and unlinks the files, so its ACCESS EXCLUSIVE lock lasts
-- milliseconds, and every query on these tables runs in about a millisecond.
--
-- Ship before or after the CloudNativePG cutover (backlog 40), not with it.
-- Before is better: the import then copies ~511 MB less.

DROP INDEX IF EXISTS modbus_response_data_job_id_idx;
DROP INDEX IF EXISTS modbus_register_integer_values_reg_addr_idx;
DROP INDEX IF EXISTS modbus_register_uinteger_values_reg_adr_idx;
