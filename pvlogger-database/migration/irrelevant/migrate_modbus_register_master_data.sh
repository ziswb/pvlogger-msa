psql \
    --host "$DST_DB_HOST" \
    --port 5432 \
    --username postgres \
    --dbname "pvmasterdata" \
    --quiet \
    --echo-errors \
    -c "TRUNCATE TABLE modbus_register_master_data;"
migrate_db_table_to_db "modbus_register_master_data" "pvmasterdata"
