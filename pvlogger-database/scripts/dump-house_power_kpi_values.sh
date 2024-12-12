#!/bin/sh

FILENAME=house_power_kpi_values_`date +%Y-%m-%d"_"%H_%M_%S`

# https://www.prisma.io/dataguide/postgresql/inserting-and-modifying-data/importing-and-exporting-data-in-postgresql
pg_dump \
  --host node01.local \
  --port 5432 \
  --user postgres \
  --dbname pvlogger \
  --table house_power_kpi_values \
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
  --file $FILENAME.sql \
  && tar -czvf $FILENAME.tar.gz $FILENAME.sql
