create table public.modbus_response_data
(
    id              serial
        primary key,
    date_created    timestamp default CURRENT_TIMESTAMP not null,
    job_id          text                                not null,
    request_time    timestamp                           not null,
    request_id      integer                             not null
        constraint chk_modbus_response_data_unsigned_request_id
            check ((request_id >= 0) AND (request_id <= 65535)),
    response_time   timestamp                           not null,
    protocol_type   integer                             not null
        constraint chk_modbus_response_data_unsigned_protocol_type
            check ((protocol_type >= 0) AND (protocol_type <= 65535)),
    data_length     integer                             not null
        constraint chk_modbus_response_data_unsigned_data_length
            check ((data_length >= 0) AND (data_length <= 65535)),
    logic_device_id smallint                            not null
        constraint chk_modbus_response_data_unsigned_byte_logic_device_id
            check ((logic_device_id >= 0) AND (logic_device_id <= 255)),
    function_code   smallint                            not null
        constraint chk_modbus_response_data_unsigned_byte_function_code
            check ((function_code >= 0) AND (function_code <= 255)),
    number_of_bytes smallint
        constraint chk_modbus_response_data_unsigned_byte_number_of_bytes
            check ((number_of_bytes >= 0) AND (number_of_bytes <= 255))
);

alter table public.modbus_response_data
    owner to postgres;

