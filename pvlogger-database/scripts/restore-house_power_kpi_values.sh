#!/bin/sh

FILENAME=house_power_kpi_values_2024-12-12_15_15_03

# https://www.prisma.io/dataguide/postgresql/inserting-and-modifying-data/importing-and-exporting-data-in-postgresql
psql \
  --host localhost \
  --port 5432 \
  --username postgres \
  --dbname pvdata \
  < $FILENAME.sql
