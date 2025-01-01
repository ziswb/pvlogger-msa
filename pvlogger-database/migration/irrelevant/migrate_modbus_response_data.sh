psql \
    --host "$DST_DB_HOST" \
    --port 5432 \
    --username postgres \
    --dbname "pvdata" \
    --echo-errors \
    -c "TRUNCATE TABLE modbus_response_data cascade;
        ALTER TABLE modbus_response_data DROP CONSTRAINT IF EXISTS chk_modbus_response_data_unsigned_request_id;
        ALTER TABLE modbus_response_data DROP CONSTRAINT IF EXISTS chk_modbus_response_data_unsigned_protocol_type;
        ALTER TABLE modbus_response_data DROP CONSTRAINT IF EXISTS chk_modbus_response_data_unsigned_data_length;
        ALTER TABLE modbus_response_data DROP CONSTRAINT IF EXISTS chk_modbus_response_data_unsigned_byte_logic_device_id;
        ALTER TABLE modbus_response_data DROP CONSTRAINT IF EXISTS chk_modbus_response_data_unsigned_byte_function_code;
        ALTER TABLE modbus_response_data DROP CONSTRAINT IF EXISTS chk_modbus_response_data_unsigned_byte_number_of_bytes;"

migrate_db_table_to_db "modbus_response_data" "pvdata"
