-- 48,  INVERTER,   48, Accumulated energy yield,       RO, U32,    KWH,    HUNDRED,    32106,  2,  N/A,                                                            DYNAMIC
-- 49,  INVERTER,   49, Daily energy yield,             RO, U32,    KWH,    HUNDRED,    32114,  2,  N/A,                                                            DYNAMIC
-- 347, METER,      12, Positive active electricity,    RO, I32,    KWH,    HUNDRED,    37119,  2,  Electricity fed by the inverter to the power grid.,             DYNAMIC
-- 348, METER,      13, Reverse active power,           RO, I32,    KWH,    HUNDRED,    37121,  2,  Power supplied to a distributed system from the power grid.,    DYNAMIC

-- exported - per day
CREATE MATERIALIZED VIEW exported_energy_view_per_day AS
SELECT dt,
       max - lag(max) OVER (ORDER BY dt) AS kwh
FROM (SELECT mrd.date_created::date                    AS dt,
             max(mriv.register_value)::numeric / 100.0 AS max
      FROM modbus_register_integer_values mriv
               JOIN modbus_response_data mrd ON mriv.response_id = mrd.id
      WHERE mriv.register_address = 37119
      GROUP BY mrd.date_created::date) tbl
ORDER BY dt DESC;

-- exported - by the minute
CREATE MATERIALIZED VIEW exported_energy_view_by_the_minute AS
SELECT dt,
       max - lag(max) OVER (ORDER BY dt) AS kwh
FROM (SELECT mrd.date_created                          AS dt,
             max(mriv.register_value)::numeric / 100.0 AS max
      FROM modbus_register_integer_values mriv
               JOIN modbus_response_data mrd ON mriv.response_id = mrd.id
      WHERE mriv.register_address = 37119
      GROUP BY mrd.date_created) tbl
ORDER BY dt DESC;

-- imported per day
CREATE MATERIALIZED VIEW imported_energy_view_per_day AS
SELECT dt,
       (kwh - lag(kwh) OVER (ORDER BY dt))::numeric / 100.0 AS kwh
FROM (SELECT mrd.date_created::date  AS dt,
             max(mriv.register_value) AS kwh
      FROM modbus_register_integer_values mriv
               JOIN modbus_response_data mrd ON mriv.response_id = mrd.id
      WHERE mriv.register_address = 37121
      GROUP BY mrd.date_created::date) tbl
ORDER BY dt DESC;

-- imported by the minute
CREATE MATERIALIZED VIEW imported_energy_view_by_the_minute AS
SELECT mrd.date_created as dt,
       (mriv.register_value - lag(mriv.register_value) OVER (ORDER BY mrd.date_created))::numeric / 100.0 AS kwh
FROM modbus_register_integer_values mriv
         JOIN modbus_response_data mrd ON mriv.response_id = mrd.id
WHERE mriv.register_address = 37121
ORDER BY mrd.date_created DESC;

-- yielded per day
CREATE MATERIALIZED VIEW yielded_energy_view_per_day AS
SELECT dt,
       sum(max) AS kwh
FROM (SELECT mrd.date_created::date                    AS dt,
             max(mriv.register_value)::numeric / 100.0 AS max
      FROM modbus_register_uinteger_values mriv
               JOIN modbus_response_data mrd ON mriv.response_id = mrd.id
      WHERE mriv.register_address = 32114
      GROUP BY (mrd.date_created::date), mrd.logic_device_id) tbl
GROUP BY dt
ORDER BY dt DESC;

-- yielded by the minute
CREATE MATERIALIZED VIEW yielded_energy_view_by_the_minute AS
SELECT dt,
       (val - lag(val) OVER (ORDER BY dt)) / 100.0 AS kwh
FROM (SELECT max(mrd.date_created)    AS dt,
             sum(mriv.register_value) AS val
      FROM modbus_register_uinteger_values mriv
               JOIN modbus_response_data mrd ON mriv.response_id = mrd.id
      WHERE mriv.register_address = 32106
      GROUP BY mrd.job_id
      HAVING count(mriv.register_value) = 2) tbl
ORDER BY dt DESC;

-- consumed per day
CREATE VIEW consumption_per_day AS
SELECT y.dt,
       y.kwh - e.kwh + i.kwh AS kwh
FROM yielded_energy_view_per_day y
         JOIN exported_energy_view_per_day e ON y.dt = e.dt
         JOIN imported_energy_view_per_day i ON e.dt = i.dt
ORDER BY y.dt DESC;

-- consumed by the minute
CREATE VIEW consumption_by_the_minute AS
SELECT y.dt,
       y.kwh - e.kwh + i.kwh AS kwh
FROM yielded_energy_view_per_day y
         JOIN exported_energy_view_by_the_minute e ON y.dt = e.dt
         JOIN imported_energy_view_by_the_minute i ON e.dt = i.dt
ORDER BY y.dt DESC;

-- from pv per day
CREATE VIEW from_pv_per_day AS
SELECT y.dt,
       y.kwh - e.kwh AS kwh
FROM yielded_energy_view_per_day y
         JOIN exported_energy_view_per_day e ON y.dt = e.dt
ORDER BY y.dt DESC;

-- from pv by the minute
CREATE VIEW from_pv_by_the_minute AS
SELECT y.dt,
       y.kwh - e.kwh AS kwh
FROM yielded_energy_view_by_the_minute y
         JOIN exported_energy_view_by_the_minute e ON y.dt = e.dt
ORDER BY y.dt DESC;
