create table public.modbus_characteristic_crv_data
(
    id                  serial
        primary key,
    response_id         integer  not null
        constraint fk_modbus_characteristic_crv_data_response_id__id
            references public.modbus_response_data
            on update restrict on delete restrict,
    register_address    integer  not null,
    number_of_points    integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_number_of_points
            check ((number_of_points >= 0) AND (number_of_points <= 65535)),
    p_value_at_point_01 integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_p_value_at_point_01
            check ((p_value_at_point_01 >= 0) AND (p_value_at_point_01 <= 65535)),
    c_value_at_point_01 smallint not null,
    p_value_at_point_02 integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_p_value_at_point_02
            check ((p_value_at_point_02 >= 0) AND (p_value_at_point_02 <= 65535)),
    c_value_at_point_02 smallint not null,
    p_value_at_point_03 integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_p_value_at_point_03
            check ((p_value_at_point_03 >= 0) AND (p_value_at_point_03 <= 65535)),
    c_value_at_point_03 smallint not null,
    p_value_at_point_04 integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_p_value_at_point_04
            check ((p_value_at_point_04 >= 0) AND (p_value_at_point_04 <= 65535)),
    c_value_at_point_04 smallint not null,
    p_value_at_point_05 integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_p_value_at_point_05
            check ((p_value_at_point_05 >= 0) AND (p_value_at_point_05 <= 65535)),
    c_value_at_point_05 smallint not null,
    p_value_at_point_06 integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_p_value_at_point_06
            check ((p_value_at_point_06 >= 0) AND (p_value_at_point_06 <= 65535)),
    c_value_at_point_06 smallint not null,
    p_value_at_point_07 integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_p_value_at_point_07
            check ((p_value_at_point_07 >= 0) AND (p_value_at_point_07 <= 65535)),
    c_value_at_point_07 smallint not null,
    p_value_at_point_08 integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_p_value_at_point_08
            check ((p_value_at_point_08 >= 0) AND (p_value_at_point_08 <= 65535)),
    c_value_at_point_08 smallint not null,
    p_value_at_point_09 integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_p_value_at_point_09
            check ((p_value_at_point_09 >= 0) AND (p_value_at_point_09 <= 65535)),
    c_value_at_point_09 smallint not null,
    p_value_at_point_10 integer  not null
        constraint chk_modbus_characteristic_crv_data_unsigned_p_value_at_point_10
            check ((p_value_at_point_10 >= 0) AND (p_value_at_point_10 <= 65535)),
    c_value_at_point_10 smallint not null
);

alter table public.modbus_characteristic_crv_data
    owner to postgres;

