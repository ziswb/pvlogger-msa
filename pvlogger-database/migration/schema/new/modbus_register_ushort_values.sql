create table public.modbus_register_ushort_values
(
    id               serial
        primary key,
    response_id      integer not null
        constraint fk_modbus_register_ushort_values_response_id__id
            references public.modbus_response_data
            on update restrict on delete restrict,
    register_address integer not null,
    register_value   bigint
        constraint chk_modbus_register_ushort_values_unsigned_register_value
            check ((register_value >= 0) AND (register_value <= '4294967295'::bigint))
);

alter table public.modbus_register_ushort_values
    owner to postgres;

