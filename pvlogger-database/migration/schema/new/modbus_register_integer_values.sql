create table public.modbus_register_integer_values
(
    id               serial
        primary key,
    response_id      integer not null
        constraint fk_modbus_register_integer_values_response_id__id
            references public.modbus_response_data
            on update restrict on delete restrict,
    register_address integer not null,
    register_value   integer
);

alter table public.modbus_register_integer_values
    owner to postgres;

