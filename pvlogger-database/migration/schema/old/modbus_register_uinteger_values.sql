create table public.modbus_register_uinteger_values
(
    id               serial
        primary key,
    response_id      integer not null
        constraint fk_modbus_register_uinteger_values_response_id__id
            references public.modbus_response_data
            on update restrict on delete restrict,
    register_address integer not null
        constraint fk_modbus_register_uinteger_values_register_address__address
            references public.modbus_register_master_data (address)
            on update restrict on delete restrict,
    register_value   bigint
);

alter table public.modbus_register_uinteger_values
    owner to postgres;

create index values_idx
    on public.modbus_register_uinteger_values (register_value);

create index response_id_idx
    on public.modbus_register_uinteger_values (response_id);

