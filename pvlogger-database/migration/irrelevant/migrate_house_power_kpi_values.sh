psql \
    --host "$DST_DB_HOST" \
    --port 5432 \
    --username postgres \
    --dbname "pvdata" \
    --quiet \
    --echo-errors \
    -c "truncate table house_power_kpi_values;
        insert into house_power_kpi_values (
          id,
          date_created,
          job_id,
          solar_to_house_power,
          grid_to_house_power,
          solar_to_grid_power,
          daily_energy_yield,
          solar_power,
          own_consumption,
          daily_energy_self_covered,
          daily_energy_exported,
          daily_energy_imported,
          daily_energy_consumed
        )
        SELECT *
        FROM dblink('dbname=pvlogger user=postgres', 'select * from house_power_kpi_values')
        AS kpis(
          id integer,
          date_created timestamp,
          job_id text,
          solar_to_house_power double precision,
          grid_to_house_power double precision,
          solar_to_grid_power double precision,
          daily_energy_yield double precision,
          solar_power double precision,
          own_consumption double precision,
          daily_energy_self_covered double precision,
          daily_energy_exported double precision,
          daily_energy_imported double precision,
          daily_energy_consumed double precision
        );"
