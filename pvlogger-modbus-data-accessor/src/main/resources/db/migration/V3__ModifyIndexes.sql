DROP INDEX IF EXISTS modbus_response_data_dte_created_idx;
CREATE INDEX IF NOT EXISTS modbus_response_data_date_created_idx ON modbus_response_data (date_created);
CREATE INDEX IF NOT EXISTS modbus_response_data_dev_id_idx ON modbus_response_data (logic_device_id);
CREATE INDEX IF NOT EXISTS modbus_register_uinteger_values_reg_adr_idx ON modbus_register_uinteger_values (register_address);
