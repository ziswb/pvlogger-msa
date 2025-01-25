CREATE INDEX CONCURRENTLY IF NOT EXISTS modbus_register_integer_values_reg_addr_idx ON modbus_register_integer_values (register_address);
CREATE INDEX CONCURRENTLY IF NOT EXISTS modbus_response_data_job_id_idx ON modbus_response_data (job_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS modbus_response_data_resp_time_idx ON modbus_response_data (response_time);
CREATE INDEX CONCURRENTLY IF NOT EXISTS modbus_response_data_dte_created_idx ON modbus_response_data (date_created);
