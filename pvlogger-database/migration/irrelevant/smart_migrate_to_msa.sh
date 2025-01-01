#!/usr/bin/env bash
# bash needed due to the use of arrays
# pg tools usage: https://www.prisma.io/dataguide/postgresql/inserting-and-modifying-data/importing-and-exporting-data-in-postgresql

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

SRC_DB_NAME="pvlogger"
SRC_DB_HOST="node01.local"
DST_DB_HOST="localhost"
DST_DB_NAME="pvlogger"

# DB Password
# Create a .pgpass file in the home directory of the account that pg_dump will run as.
# The format is: hostname:port:database:username:password
# Then, set the file's mode to 0600. Otherwise, it will be ignored: chmod 600 ~/.pgpass
# See the Postgresql documentation libpq-pgpass for more details.

DB_TABLES_TO_MIGRATE=(
  "modbus_register_integer_values"
  "modbus_characteristic_curve_data"
  "modbus_response_data"
  "modbus_register_short_values"
  "modbus_register_string_values"
  "modbus_register_master_data"
  "modbus_register_uinteger_values"
  "modbus_register_ushort_values"
  "house_power_kpi_values"
)

function migrate_db_table_to_db() {
  local table="$1"
  local dst_db_name="$2"
  echo "migrate_db_table_to_db: transferring data to db '$dst_db_name' - source table: '$table'"
  pg_dump \
    --host "$SRC_DB_HOST" \
    --port 5432 \
    --user postgres \
    --dbname "$SRC_DB_NAME" \
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
  | \
  psql \
    --host "$DST_DB_HOST" \
    --port 5432 \
    --username postgres \
    --dbname "$dst_db_name" \
    --quiet \
    --echo-errors
}

function copy_db() {
  echo "copying selected tables from ${SRC_DB_HOST}:${SRC_DB_NAME} db to '${DST_DB_HOST}:${DST_DB_NAME}'"
  pg_dump \
    --host "$SRC_DB_HOST" \
    --port 5432 \
    --user postgres \
    --dbname "$SRC_DB_NAME" \
    --create \
    --no-comments \
    --table modbus_register_integer_values \
    --table modbus_characteristic_curve_data \
    --table modbus_response_data \
    --table modbus_register_short_values \
    --table modbus_register_string_values \
    --table modbus_register_master_data \
  | \
  psql \
    --host "$DST_DB_HOST" \
    --port 5432 \
    --username postgres \
    --quiet \
    --echo-errors
}

psql \
    --host "$DST_DB_HOST" \
    --port 5432 \
    --username postgres \
    -c "DROP DATABASE IF EXISTS $DST_DB_NAME;"

copy_db
