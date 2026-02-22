module pvlogger-modbus-data-collector

go 1.25.4

require github.com/google/uuid v1.6.0

require pv-logger v0.0.0

require pvlogger-shared-modbus v0.0.0

replace pv-logger => ../pv-logger

replace pvlogger-shared-modbus => ../pvlogger-shared-modbus
