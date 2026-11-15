#!/usr/bin/env bash
# bash needed due to the use of arrays
# pg tools usage: https://www.prisma.io/dataguide/postgresql/inserting-and-modifying-data/importing-and-exporting-data-in-postgresql

# exit on error
set -e
# exit on undefined variables
set -u
# return the command that was the reason behind the failure
set -o pipefail

SRC_DB_HOST="localhost"
SRC_DB_PORT=5432

DST_DB_HOST="${DST_DB_HOST:?set DST_DB_HOST to the target database host}"
DST_DB_PORT="${DST_DB_PORT:-5433}"

DBS_TO_MIGRATE=(
  "fsdata"
  "heartbeat"
  "pvmasterdata"
  "pvdata"
  "sunrisesunset"
)

# DB Password
# Create a .pgpass file in the home directory of the account that pg_dump will run as.
# The format is: hostname:port:database:username:password
# Then, set the file's mode to 0600. Otherwise, it will be ignored: chmod 600 ~/.pgpass
# See the Postgresql documentation libpq-pgpass for more details.

function migrate_db() {
  local db_name="$1"
  local dumpfile="${db_name}.gz"
  if [ ! -f "${dumpfile}" ]; then
    pg_dump --verbose --host "${SRC_DB_HOST}" --port "${SRC_DB_PORT}" --user postgres --dbname "${db_name}" | gzip > "${dumpfile}"
  fi
  dropdb --host "${DST_DB_HOST}" --port "${DST_DB_PORT}" --username postgres --if-exists "${db_name}"
  createdb --host "${DST_DB_HOST}" --port "${DST_DB_PORT}" --username postgres -T template0 "${db_name}"
  gunzip -c "${dumpfile}" | psql --host "${DST_DB_HOST}" --port "${DST_DB_PORT}" --username postgres --dbname "${db_name}"
}

for db in "${DBS_TO_MIGRATE[@]}"; do
  migrate_db "$db"
done
