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

function copy_db() {
  echo "transferring db to '${DST_DB_HOST}:${DST_DB_NAME}'"
  pg_dump \
    --host "$SRC_DB_HOST" \
    --port 5432 \
    --user postgres \
    --dbname "$SRC_DB_NAME" \
    --create \
    --no-comments \
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
