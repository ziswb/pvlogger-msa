create table public.modbus_response_data
(
    id              serial
        primary key,
    date_created    timestamp default CURRENT_TIMESTAMP not null,
    job_id          text                                not null,
    request_time    timestamp                           not null,
    request_id      integer                             not null,
    response_time   timestamp                           not null,
    protocol_type   smallint                            not null,
    data_length     smallint                            not null,
    logic_device_id smallint                            not null,
    function_code   smallint                            not null,
    number_of_bytes smallint
);

alter table public.modbus_response_data
    owner to postgres;

