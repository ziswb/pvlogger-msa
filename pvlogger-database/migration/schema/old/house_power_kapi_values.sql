create table public.house_power_kpi_values
(
    id                        serial
        primary key,
    date_created              timestamp        default CURRENT_TIMESTAMP       not null,
    job_id                    text             default 'NO-JOB-ID-GIVEN'::text not null,
    solar_to_house_power      double precision default 0.0                     not null,
    grid_to_house_power       double precision default 0.0                     not null,
    solar_to_grid_power       double precision default 0.0                     not null,
    daily_energy_yield        double precision default 0.0                     not null,
    solar_power               double precision default 0.0                     not null,
    own_consumption           double precision default 0.0                     not null,
    daily_energy_self_covered double precision default 0.0                     not null,
    daily_energy_exported     double precision default 0.0                     not null,
    daily_energy_imported     double precision default 0.0                     not null,
    daily_energy_consumed     double precision default 0.0                     not null
);

alter table public.house_power_kpi_values
    owner to postgres;

