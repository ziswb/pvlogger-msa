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

# DB Password
# Create a .pgpass file in the home directory of the account that pg_dump will run as.
# The format is: hostname:port:database:username:password
# Then, set the file's mode to 0600. Otherwise, it will be ignored: chmod 600 ~/.pgpass
# See the Postgresql documentation libpq-pgpass for more details.

PV_DATA_DB_TABLES=(
  "modbus_response_data"
  "modbus_characteristic_curve_data"
  "modbus_register_integer_values"
  "modbus_register_short_values"
  "modbus_register_string_values"
  "modbus_register_uinteger_values"
  "modbus_register_ushort_values"
)

MASTER_DATA_DB_TABLES=(
  "modbus_register_master_data"
)

KPI_DATA_DB_TABLES=("house_power_kpi_values")

function migrate_db_table_to_db() {
  local table="$1"
  local dst_db_name="$2"
  echo "transferring data to db '$dst_db_name' - source table: '$table'"
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

for table in "${MASTER_DATA_DB_TABLES[@]}"; do
  migrate_db_table_to_db $table "pvmasterdata"
done

for table in "${PV_DATA_DB_TABLES[@]}"; do
  migrate_db_table_to_db $table "pvdata"
done

for table in "${KPI_DATA_DB_TABLES[@]}"; do
  echo "migrate_db_table_to_db $table \"pvdata\""
  echo "transferring data to db '$dst_db_name' - source table: '$table'"
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
    --column-inserts \
    --disable-triggers \
  | \
  psql \
    --host "$DST_DB_HOST" \
    --port 5432 \
    --username postgres \
    --dbname "$dst_db_name" \
    --quiet \
    --echo-errors
done
