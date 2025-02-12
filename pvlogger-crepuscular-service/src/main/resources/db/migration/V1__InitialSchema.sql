CREATE TABLE solar_table
(
    id                          serial
        primary key,
    status                      text    not null,
    sunrise                     text    not null,
    sunset                      text    not null,
    solar_noon                  text    not null,
    day_length                  integer not null,
    civil_twilight_begin        text    not null,
    civil_twilight_end          text    not null,
    nautical_twilight_begin     text    not null,
    nautical_twilight_end       text    not null,
    astronomical_twilight_begin text    not null,
    astronomical_twilight_end   text    not null
);
