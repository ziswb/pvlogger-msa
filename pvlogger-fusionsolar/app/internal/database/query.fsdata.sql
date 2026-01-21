-- name: GetFsIds :many
SELECT id
FROM fs_response
WHERE id < 100;

-- name: InsertFsResponse :one
INSERT INTO fs_response (fail_code, message, raw, success, timestamp)
VALUES ($1, $2, $3, $4, $5)
RETURNING *;

-- name: InsertFsResponseParamsBasic :exec
INSERT INTO fs_response_params_basic ("current_time", response, station_codes)
VALUES ($1, $2, $3);

-- name: InsertFsStationRealKpi :exec
INSERT INTO fs_station_real_kpi (day_income,
                                 day_power,
                                 month_power,
                                 real_health_state,
                                 response,
                                 station_code,
                                 total_income,
                                 total_power,
                                 type)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9);

-- name: InsertFsResponseParamsDevice :exec
INSERT INTO fs_response_params_device ("current_time", dev_ids, dev_type_id, response)
VALUES ($1, $2, $3, $4);

-- name: InsertFsDeviceRealKpi :exec
INSERT INTO fs_device_real_kpi (a_i, a_u, ab_u, active_power, b_i, b_u, bc_u, c_i, c_u, ca_u, close_time, day_cap,
                                "devId", efficiency, elec_freq, inverter_state, mppt_10_cap, mppt_1_cap, mppt_2_cap,
                                mppt_3_cap, mppt_4_cap, mppt_5_cap, mppt_6_cap, mppt_7_cap, mppt_8_cap, mppt_9_cap,
                                mppt_power, mppt_total_cap, open_time, power_factor, pv10_i, pv10_u, pv11_i, pv11_u,
                                pv12_i, pv12_u, pv13_i, pv13_u, pv14_i, pv14_u, pv15_i, pv15_u, pv16_i, pv16_u, pv17_i,
                                pv17_u, pv18_i, pv18_u, pv19_i, pv19_u, pv1_i, pv1_u, pv20_i, pv20_u, pv21_i, pv21_u,
                                pv22_i, pv22_u, pv23_i, pv23_u, pv24_i, pv24_u, pv25_i, pv25_u, pv26_i, pv26_u, pv27_i,
                                pv27_u, pv28_i, pv28_u, pv2_i, pv2_u, pv3_i, pv3_u, pv4_i, pv4_u, pv5_i, pv5_u, pv6_i,
                                pv6_u, pv7_i, pv7_u, pv8_i, pv8_u, pv9_i, pv9_u, reactive_power, response, run_state,
                                temperature, total_cap, type)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22, $23, $24,
        $25, $26, $27, $28, $29, $30, $31, $32, $33, $34, $35, $36, $37, $38, $39, $40, $41, $42, $43, $44, $45, $46,
        $47, $48, $49, $50, $51, $52, $53, $54, $55, $56, $57, $58, $59, $60, $61, $62, $63, $64, $65, $66, $67, $68,
        $69, $70, $71, $72, $73, $74, $75, $76, $77, $78, $79, $80, $81, $82, $83, $84, $85, $86, $87, $88, $89, $90,
        $91, $92) ON CONFLICT DO NOTHING;

-- name: InsertFsResponseParamsExtended :exec
INSERT INTO fs_response_params_extended (collect_time, "current_time", response, station_codes)
VALUES ($1, $2, $3, $4);

-- name: UpsertFsStationExtendedData :exec
INSERT INTO fs_station_extended_data (collect_time, installed_capacity, inverter_power, ongrid_power, performance_ratio,
                                      perpower_ratio, power_profit, radiation_intensity, reduction_total_co2,
                                      reduction_total_coal, reduction_total_tree, station_code, theory_power, "type",
                                      use_power)
VALUES ($1,
        $2,
        $3,
        $4,
        $5,
        $6,
        $7,
        $8,
        $9,
        $10,
        $11,
        $12,
        $13,
        $14,
        $15) ON CONFLICT(response_id, collect_time, "type")
DO
UPDATE SET
    installed_capacity=$2,
    inverter_power=$3,
    ongrid_power=$4,
    performance_ratio=$5,
    perpower_ratio=$6,
    power_profit=$7,
    radiation_intensity=$8,
    reduction_total_co2=$9,
    reduction_total_coal=$10,
    reduction_total_tree=$11,
    station_code=$12,
    theory_power=$13,
    use_power=$15;

-- name: GetFsStationExtendedDataIds :many
-- STATION_DAY_KPI, STATION_MONTH_KPI, STATION_YEAR_KPI
SELECT fs_response_to_station_extended_data.response_id
FROM fs_response_to_station_extended_data
WHERE (fs_response_to_station_extended_data.collect_time = $1)
  AND (fs_response_to_station_extended_data."type" = $2);

-- name: UpdateFsResponseToStationExtendedData :exec
UPDATE fs_response_to_station_extended_data
SET response_id=$1
WHERE fs_response_to_station_extended_data.response_id = $2;

-- name: InsertFsStationExtendedDataMapping :exec
-- STATION_DAY_KPI, STATION_MONTH_KPI, STATION_YEAR_KPI
-- the data model is broken with this table; it makes no sense and needs refactoring
INSERT INTO fs_response_to_station_extended_data (response_id, collect_time, "type")
VALUES ($1, $2, $3);
