create table public.modbus_register_short_values
(
    id               serial
        primary key,
    response_id      integer not null
        constraint fk_modbus_register_short_values_response_id__id
            references public.modbus_response_data
            on update restrict on delete restrict,
    register_address integer not null
        constraint fk_modbus_register_short_values_register_address__address
            references public.modbus_register_master_data (address)
            on update restrict on delete restrict,
    register_value   smallint
);

alter table public.modbus_register_short_values
    owner to postgres;

