SELECT MAX(id) FROM modbus_response_data;
SELECT nextval('modbus_response_data_id_seq');
BEGIN;
LOCK TABLE modbus_response_data IN EXCLUSIVE MODE;
SELECT setval('modbus_response_data_id_seq', (SELECT GREATEST(MAX(id), nextval('modbus_response_data_id_seq') - 1) FROM modbus_response_data));
COMMIT;
END;

SELECT MAX(id) FROM house_power_kpi_values;
SELECT nextval('house_power_kpi_values_id_seq');
BEGIN;
LOCK TABLE house_power_kpi_values IN EXCLUSIVE MODE;
SELECT setval('house_power_kpi_values_id_seq', (SELECT GREATEST(MAX(id), nextval('house_power_kpi_values_id_seq') - 1) FROM house_power_kpi_values));
COMMIT;
END;

SELECT MAX(id) FROM modbus_characteristic_curve_data;
SELECT nextval('modbus_characteristic_curve_data_id_seq');
BEGIN;
LOCK TABLE modbus_characteristic_curve_data IN EXCLUSIVE MODE;
SELECT setval('modbus_characteristic_curve_data_id_seq', (SELECT GREATEST(MAX(id), nextval('modbus_characteristic_curve_data_id_seq') - 1) FROM modbus_characteristic_curve_data));
COMMIT;
END;

SELECT MAX(id) FROM modbus_register_integer_values;
SELECT nextval('modbus_register_integer_values_id_seq');
BEGIN;
LOCK TABLE modbus_register_integer_values IN EXCLUSIVE MODE;
SELECT setval('modbus_register_integer_values_id_seq', (SELECT GREATEST(MAX(id), nextval('modbus_register_integer_values_id_seq') - 1) FROM modbus_register_integer_values));
COMMIT;
END;

SELECT MAX(id) FROM modbus_register_short_values;
SELECT nextval('modbus_register_short_values_id_seq');
BEGIN;
LOCK TABLE modbus_register_short_values IN EXCLUSIVE MODE;
SELECT setval('modbus_register_short_values_id_seq', (SELECT GREATEST(MAX(id), nextval('modbus_register_short_values_id_seq') - 1) FROM modbus_register_short_values));
COMMIT;
END;

SELECT MAX(id) FROM modbus_register_string_values;
SELECT nextval('modbus_register_string_values_id_seq');
BEGIN;
LOCK TABLE modbus_register_string_values IN EXCLUSIVE MODE;
SELECT setval('modbus_register_string_values_id_seq', (SELECT GREATEST(MAX(id), nextval('modbus_register_string_values_id_seq') - 1) FROM modbus_register_string_values));
COMMIT;
END;

SELECT MAX(id) FROM modbus_register_uinteger_values;
SELECT nextval('modbus_register_uinteger_values_id_seq');
BEGIN;
LOCK TABLE modbus_register_uinteger_values IN EXCLUSIVE MODE;
SELECT setval('modbus_register_uinteger_values_id_seq', (SELECT GREATEST(MAX(id), nextval('modbus_register_uinteger_values_id_seq') - 1) FROM modbus_register_uinteger_values));
COMMIT;
END;

SELECT MAX(id) FROM public.modbus_register_ushort_values;
SELECT nextval('public.modbus_register_ushort_values_id_seq');
BEGIN;
LOCK TABLE modbus_register_ushort_values IN EXCLUSIVE MODE;
SELECT setval('modbus_register_ushort_values_id_seq', (SELECT GREATEST(MAX(id), nextval('modbus_register_ushort_values_id_seq') - 1) FROM modbus_register_ushort_values));
COMMIT;
END;