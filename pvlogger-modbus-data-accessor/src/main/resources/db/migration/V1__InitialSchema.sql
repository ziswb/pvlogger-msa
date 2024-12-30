CREATE TABLE house_power_kpi_values
(
    id                        SERIAL PRIMARY key,
    date_created              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP       NOT NULL,
    job_id                    TEXT             DEFAULT 'NO-JOB-ID-GIVEN'::text NOT NULL,
    solar_to_house_power      DOUBLE PRECISION DEFAULT 0.0                     NOT NULL,
    grid_to_house_power       DOUBLE PRECISION DEFAULT 0.0                     NOT NULL,
    solar_to_grid_power       DOUBLE PRECISION DEFAULT 0.0                     NOT NULL,
    daily_energy_yield        DOUBLE PRECISION DEFAULT 0.0                     NOT NULL,
    solar_power               DOUBLE PRECISION DEFAULT 0.0                     NOT NULL,
    own_consumption           DOUBLE PRECISION DEFAULT 0.0                     NOT NULL,
    daily_energy_self_covered DOUBLE PRECISION DEFAULT 0.0                     NOT NULL,
    daily_energy_exported     DOUBLE PRECISION DEFAULT 0.0                     NOT NULL,
    daily_energy_imported     DOUBLE PRECISION DEFAULT 0.0                     NOT NULL,
    daily_energy_consumed     DOUBLE PRECISION DEFAULT 0.0                     NOT NULL
);

CREATE INDEX house_power_kpi_values_date_created_idx ON house_power_kpi_values (date_created);

CREATE TABLE modbus_response_data
(
    id              SERIAL PRIMARY KEY,
    date_created    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    job_id          TEXT                                NOT NULL,
    request_time    TIMESTAMP                           NOT NULL,
    request_id      INTEGER                             NOT NULL,
    response_time   TIMESTAMP                           NOT NULL,
    protocol_type   SMALLINT                            NOT NULL,
    data_length     SMALLINT                            NOT NULL,
    logic_device_id SMALLINT                            NOT NULL,
    function_code   SMALLINT                            NOT NULL,
    number_of_bytes SMALLINT
);

CREATE INDEX modbus_response_data_date_created_idx ON modbus_response_data (date_created);
CREATE INDEX modbus_response_data_request_time_idx ON modbus_response_data (request_time);
CREATE INDEX modbus_response_data_response_time_idx ON modbus_response_data (response_time);

CREATE TABLE modbus_characteristic_curve_data
(
    id                  SERIAL PRIMARY KEY,
    response_id         INTEGER  NOT NULL
        CONSTRAINT fk_modbus_characteristic_curve_data_response_id__id
            REFERENCES modbus_response_data
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    register_address    INTEGER  NOT NULL,
    number_of_points    SMALLINT NOT NULL,
    p_value_at_point_01 SMALLINT NOT NULL,
    c_value_at_point_01 SMALLINT NOT NULL,
    p_value_at_point_02 SMALLINT NOT NULL,
    c_value_at_point_02 SMALLINT NOT NULL,
    p_value_at_point_03 SMALLINT NOT NULL,
    c_value_at_point_03 SMALLINT NOT NULL,
    p_value_at_point_04 SMALLINT NOT NULL,
    c_value_at_point_04 SMALLINT NOT NULL,
    p_value_at_point_05 SMALLINT NOT NULL,
    c_value_at_point_05 SMALLINT NOT NULL,
    p_value_at_point_06 SMALLINT NOT NULL,
    c_value_at_point_06 SMALLINT NOT NULL,
    p_value_at_point_07 SMALLINT NOT NULL,
    c_value_at_point_07 SMALLINT NOT NULL,
    p_value_at_point_08 SMALLINT NOT NULL,
    c_value_at_point_08 SMALLINT NOT NULL,
    p_value_at_point_09 SMALLINT NOT NULL,
    c_value_at_point_09 SMALLINT NOT NULL,
    p_value_at_point_10 SMALLINT NOT NULL,
    c_value_at_point_10 SMALLINT NOT NULL
);

CREATE TABLE modbus_register_integer_values
(
    id               SERIAL PRIMARY KEY,
    response_id      INTEGER NOT NULL
        CONSTRAINT fk_modbus_register_integer_values_response_id__id
            REFERENCES modbus_response_data
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    register_address INTEGER NOT NULL,
    register_value   INTEGER
);

CREATE TABLE modbus_register_short_values
(
    id               SERIAL PRIMARY KEY,
    response_id      INTEGER NOT NULL
        CONSTRAINT fk_modbus_register_short_values_response_id__id
            REFERENCES modbus_response_data
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    register_address INTEGER NOT NULL,
    register_value   SMALLINT
);

CREATE TABLE modbus_register_string_values
(
    id               SERIAL PRIMARY KEY,
    response_id      INTEGER NOT NULL
        CONSTRAINT fk_modbus_register_string_values_response_id__id
            REFERENCES modbus_response_data
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    register_address INTEGER NOT NULL,
    register_value   TEXT
);

CREATE TABLE modbus_register_uinteger_values
(
    id               SERIAL PRIMARY KEY,
    response_id      INTEGER NOT NULL
        CONSTRAINT fk_modbus_register_uinteger_values_response_id__id
            REFERENCES modbus_response_data
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    register_address INTEGER NOT NULL,
    register_value   BIGINT
);

CREATE TABLE modbus_register_ushort_values
(
    id               SERIAL PRIMARY KEY,
    response_id      INTEGER NOT NULL
        CONSTRAINT fk_modbus_register_ushort_values_response_id__id
            REFERENCES modbus_response_data
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    register_address INTEGER NOT NULL,
    register_value   INTEGER
);
