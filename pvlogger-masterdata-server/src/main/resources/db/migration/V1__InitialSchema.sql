create table public.modbus_register_master_data
(
    id          serial primary key,
    device_type varchar(8)  not null,
    no          integer     not null,
    name        text        not null,
    read_write  varchar(2)  not null,
    type        varchar(11) not null,
    unit        varchar(18) not null,
    gain        varchar(8)  not null,
    address     integer     not null
        constraint modbus_register_master_data_address_unique unique,
    quantity    integer     not null,
    scope       text,
    category    varchar(7)  not null
);
