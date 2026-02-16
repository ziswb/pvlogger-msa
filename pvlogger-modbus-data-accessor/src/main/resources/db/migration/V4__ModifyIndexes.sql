CREATE INDEX IF NOT EXISTS modbus_register_integer_value_idx ON modbus_register_integer_values (register_address, response_id);
DROP INDEX IF EXISTS modbus_response_data_resp_time_idx;
