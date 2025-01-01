psql \
    --host "$DST_DB_HOST" \
    --port 5432 \
    --username postgres \
    --dbname "pvdata" \
    --quiet \
    --echo-errors \
    -c "TRUNCATE TABLE modbus_register_string_values;"
migrate_db_table_to_db "modbus_register_string_values" "pvdata"
