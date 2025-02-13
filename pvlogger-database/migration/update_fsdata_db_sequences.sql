SELECT MAX(id) FROM fs_device_real_kpi;
SELECT nextval('fs_device_real_kpi_id_seq');
BEGIN;
LOCK TABLE fs_device_real_kpi IN EXCLUSIVE MODE;
SELECT setval('fs_device_real_kpi_id_seq', (SELECT GREATEST(MAX(id), nextval('fs_device_real_kpi_id_seq') - 1) FROM fs_device_real_kpi));
COMMIT;
END;

SELECT MAX(id) FROM fs_response;
SELECT nextval('fs_response_id_seq');
BEGIN;
LOCK TABLE fs_response IN EXCLUSIVE MODE;
SELECT setval('fs_response_id_seq', (SELECT GREATEST(MAX(id), nextval('fs_response_id_seq') - 1) FROM fs_response));
COMMIT;
END;

SELECT MAX(id) FROM fs_response_params_basic;
SELECT nextval('fs_response_params_basic_id_seq');
BEGIN;
LOCK TABLE fs_response_params_basic IN EXCLUSIVE MODE;
SELECT setval('fs_response_params_basic_id_seq', (SELECT GREATEST(MAX(id), nextval('fs_response_params_basic_id_seq') - 1) FROM fs_response_params_basic));
COMMIT;
END;

SELECT MAX(id) FROM fs_response_params_device;
SELECT nextval('fs_response_params_device_id_seq');
BEGIN;
LOCK TABLE fs_response_params_device IN EXCLUSIVE MODE;
SELECT setval('fs_response_params_device_id_seq', (SELECT GREATEST(MAX(id), nextval('fs_response_params_device_id_seq') - 1) FROM fs_response_params_device));
COMMIT;
END;

SELECT MAX(id) FROM fs_response_params_extended;
SELECT nextval('fs_response_params_extended_id_seq');
BEGIN;
LOCK TABLE fs_response_params_extended IN EXCLUSIVE MODE;
SELECT setval('fs_response_params_extended_id_seq', (SELECT GREATEST(MAX(id), nextval('fs_response_params_extended_id_seq') - 1) FROM fs_response_params_extended));
COMMIT;
END;

SELECT MAX(id) FROM public.fs_station_real_kpi;
SELECT nextval('fs_station_real_kpi_id_seq');
BEGIN;
LOCK TABLE fs_station_real_kpi IN EXCLUSIVE MODE;
SELECT setval('fs_station_real_kpi_id_seq', (SELECT GREATEST(MAX(id), nextval('fs_station_real_kpi_id_seq') - 1) FROM fs_station_real_kpi));
COMMIT;
END;