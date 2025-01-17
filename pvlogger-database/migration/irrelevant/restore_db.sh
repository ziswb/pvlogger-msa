#!/usr/bin/env bash
# bash needed due to the use of arrays
# pg tools usage: https://www.prisma.io/dataguide/postgresql/inserting-and-modifying-data/importing-and-exporting-data-in-postgresql

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

DST_DB_HOST="localhost"
DST_DB_PORT=5432
FILE_EXT="dump"

# DB Password
# Create a .pgpass file in the home directory of the account that pg_dump will run as.
# The format is: hostname:port:database:username:password
# Then, set the file's mode to 0600. Otherwise, it will be ignored: chmod 600 ~/.pgpass
# See the Postgresql documentation libpq-pgpass for more details.

PVDATA_DATA_TO_COPY=(
  "modbus_register_integer_values"
  "modbus_characteristic_curve_data"
  "modbus_response_data"
  "modbus_register_short_values"
  "modbus_register_string_values"
  "modbus_register_uinteger_values"
  "modbus_register_ushort_values"
  "house_power_kpi_values"
  )

MASTERDATA_DATA_TO_COPY=(
  "modbus_register_master_data"
  )

function restore_data() {
  local table="$1"
  local dst_db_name="$2"
  echo "truncating data from '${DST_DB_HOST}:${DST_DB_PORT}:${dst_db_name}.${table}'"
  psql \
      --host "$DST_DB_HOST" \
      --port "$DST_DB_PORT" \
      --username postgres \
      --dbname "$dst_db_name" \
      --echo-errors \
      -c "TRUNCATE TABLE ${table} cascade;"
  echo "restoring data from '${table}.${FILE_EXT}' to '${DST_DB_HOST}:${DST_DB_PORT}:${dst_db_name}.${table}'"
  pg_restore \
    --host "$DST_DB_HOST" \
    --port "$DST_DB_PORT" \
    --user postgres \
    --dbname "$dst_db_name" \
    --table "$table" \
    --no-owner \
    --data-only \
    --no-comments \
    --no-privileges \
    --no-tablespaces \
    --no-publications \
    --no-subscriptions \
    --no-security-labels \
    --no-table-access-method \
    --disable-triggers \
    "${table}.${FILE_EXT}"
}

for table in "${PVDATA_DATA_TO_COPY[@]}"; do
  restore_data "$table" "pvdata"
done

for table in "${MASTERDATA_DATA_TO_COPY[@]}"; do
  restore_data "$table" "pvmasterdata"
done
