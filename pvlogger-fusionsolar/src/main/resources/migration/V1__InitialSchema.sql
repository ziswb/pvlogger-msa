create table public.fs_device_real_kpi
(
    id             serial
        primary key,
    response       integer not null
        constraint fk_fs_device_real_kpi_response__id
            references public.fs_response
            on update restrict on delete restrict,
    type           varchar(17) default 'DEVICE_REAL_KPI'::character varying not null,
    "devId"        bigint,
    pv26_i         double precision,
    pv2_u          double precision,
    pv28_i         double precision,
    pv4_u          double precision,
    pv22_i         double precision,
    power_factor   double precision,
    pv6_u          double precision,
    pv24_i         double precision,
    mppt_total_cap double precision,
    pv8_u          double precision,
    pv22_u         double precision,
    open_time      bigint,
    a_i            double precision,
    pv24_u         double precision,
    c_i            double precision,
    mppt_9_cap     double precision,
    pv20_u         double precision,
    pv19_u         double precision,
    pv15_u         double precision,
    a_u            double precision,
    reactive_power double precision,
    pv17_u         double precision,
    c_u            double precision,
    mppt_8_cap     double precision,
    pv20_i         double precision,
    pv15_i         double precision,
    efficiency     double precision,
    pv17_i         double precision,
    pv11_i         double precision,
    pv13_i         double precision,
    pv11_u         double precision,
    mppt_power     double precision,
    pv13_u         double precision,
    run_state      integer,
    close_time     bigint,
    pv19_i         double precision,
    mppt_7_cap     double precision,
    mppt_5_cap     double precision,
    pv27_u         double precision,
    pv2_i          double precision,
    active_power   double precision,
    pv4_i          double precision,
    pv6_i          double precision,
    pv8_i          double precision,
    mppt_6_cap     double precision,
    pv27_i         double precision,
    pv1_u          double precision,
    pv3_u          double precision,
    pv23_i         double precision,
    pv5_u          double precision,
    pv25_i         double precision,
    pv7_u          double precision,
    pv23_u         double precision,
    inverter_state double precision,
    pv9_u          double precision,
    pv25_u         double precision,
    total_cap      double precision,
    mppt_3_cap     double precision,
    b_i            double precision,
    pv21_u         double precision,
    mppt_10_cap    double precision,
    pv16_u         double precision,
    pv18_u         double precision,
    temperature    double precision,
    b_u            double precision,
    bc_u           double precision,
    pv21_i         double precision,
    elec_freq      double precision,
    mppt_4_cap     double precision,
    pv16_i         double precision,
    pv18_i         double precision,
    day_cap        double precision,
    pv12_i         double precision,
    pv14_i         double precision,
    pv12_u         double precision,
    mppt_1_cap     double precision,
    pv14_u         double precision,
    pv10_u         double precision,
    pv26_u         double precision,
    pv1_i          double precision,
    pv28_u         double precision,
    pv3_i          double precision,
    mppt_2_cap     double precision,
    pv5_i          double precision,
    ca_u           double precision,
    ab_u           double precision,
    pv7_i          double precision,
    pv10_i         double precision,
    pv9_i          double precision,
    collect_time   bigint
);

create table public.fs_response
(
    id        serial
        primary key,
    timestamp timestamp not null,
    success   boolean   not null,
    fail_code integer   not null,
    message   text,
    raw       text      not null
);

create table public.fs_response_params_basic
(
    id             serial
        primary key,
    response       integer not null
        constraint fk_fs_response_params_basic_response__id
            references public.fs_response
            on update restrict on delete restrict,
    station_codes  text,
    "current_time" bigint
);

create table public.fs_response_params_device
(
    id             serial
        primary key,
    response       integer not null
        constraint fk_fs_response_params_device_response__id
            references public.fs_response
            on update restrict on delete restrict,
    dev_ids        text,
    dev_type_id    integer,
    "current_time" bigint
);

create table public.fs_response_params_extended
(
    id             serial
        primary key,
    response       integer not null
        constraint fk_fs_response_params_extended_response__id
            references public.fs_response
            on update restrict on delete restrict,
    station_codes  text,
    collect_time   bigint,
    "current_time" bigint
);

create table public.fs_response_to_station_extended_data
(
    response_id  integer     not null
        constraint fk_fs_response_to_station_extended_data_response_id__id
            references public.fs_response
            on update restrict on delete restrict,
    collect_time bigint      not null,
    type         varchar(17) not null,
    constraint pk_fs_response_to_station_extended_data
        primary key (response_id, collect_time, type),
    constraint fk_response_to_station_extended_data
        foreign key (collect_time, type) references public.fs_station_extended_data
);

create table public.fs_station_extended_data
(
    collect_time         bigint      not null,
    type                 varchar(17) not null,
    station_code         text        not null,
    installed_capacity   double precision,
    radiation_intensity  double precision,
    theory_power         double precision,
    performance_ratio    double precision,
    inverter_power       double precision,
    ongrid_power         double precision,
    use_power            double precision,
    power_profit         double precision,
    perpower_ratio       double precision,
    reduction_total_co2  double precision,
    reduction_total_coal double precision,
    reduction_total_tree double precision,
    constraint pk_fs_station_extended_data
        primary key (collect_time, type)
);

create index fs_station_extended_data_collect_time
    on public.fs_station_extended_data (collect_time);

create index fs_station_extended_data_type
    on public.fs_station_extended_data (type);

create table public.fs_station_real_kpi
(
    id                serial
        primary key,
    response          integer          not null
        constraint fk_fs_station_real_kpi_response__id
            references public.fs_response
            on update restrict on delete restrict,
    type              varchar(17) default 'STATION_REAL_KPI'::character varying not null,
    station_code      text,
    day_power         double precision not null,
    month_power       double precision not null,
    total_power       double precision not null,
    day_income        double precision not null,
    total_income      double precision not null,
    real_health_state integer          not null
);
