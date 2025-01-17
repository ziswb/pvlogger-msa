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
SRC_DB_PORT=5432
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

function copy_data() {
  local table="$1"
  echo "dumping data '${SRC_DB_HOST}:${SRC_DB_PORT}:${SRC_DB_NAME}.${table}'"
  pg_dump \
    --format c \
    --host "$SRC_DB_HOST" \
    --port "$SRC_DB_PORT" \
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
    --verbose \
    > "${table}.${FILE_EXT}"
}
#
#for table in "${PVDATA_DATA_TO_COPY[@]}"; do
#  copy_data "$table" "pvdata"
#done

for table in "${MASTERDATA_DATA_TO_COPY[@]}"; do
  copy_data "$table" "pvmasterdata"
done
