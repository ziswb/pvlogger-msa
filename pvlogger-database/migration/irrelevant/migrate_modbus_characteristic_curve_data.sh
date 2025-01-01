psql \
    --host "$DST_DB_HOST" \
    --port 5432 \
    --username postgres \
    --dbname "pvdata" \
    --echo-errors \
    -c "TRUNCATE TABLE modbus_characteristic_curve_data;
    create table if not exists public.modbus_characteristic_curve_data
    (
        id                  serial
            primary key,
        response_id         integer  not null
            constraint fk_modbus_characteristic_curve_data_response_id__id
                references public.modbus_response_data
                on update restrict on delete restrict,
        register_address    integer  not null,
        number_of_points    smallint not null,
        p_value_at_point_01 smallint not null,
        c_value_at_point_01 smallint not null,
        p_value_at_point_02 smallint not null,
        c_value_at_point_02 smallint not null,
        p_value_at_point_03 smallint not null,
        c_value_at_point_03 smallint not null,
        p_value_at_point_04 smallint not null,
        c_value_at_point_04 smallint not null,
        p_value_at_point_05 smallint not null,
        c_value_at_point_05 smallint not null,
        p_value_at_point_06 smallint not null,
        c_value_at_point_06 smallint not null,
        p_value_at_point_07 smallint not null,
        c_value_at_point_07 smallint not null,
        p_value_at_point_08 smallint not null,
        c_value_at_point_08 smallint not null,
        p_value_at_point_09 smallint not null,
        c_value_at_point_09 smallint not null,
        p_value_at_point_10 smallint not null,
        c_value_at_point_10 smallint not null
    );

    alter table public.modbus_characteristic_curve_data
        owner to postgres;

    drop table if exists modbus_cc_data;"

migrate_db_table_to_db "modbus_characteristic_curve_data" "pvdata"
