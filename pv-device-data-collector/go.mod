module pv-device-data-collector

go 1.27.3

require github.com/google/uuid v1.6.0 // indirect

require (
	pv-logger v0.0.0
	pvlogger-shared-modbus v0.0.0
)

replace pv-logger => ../pv-logger

replace pvlogger-shared-modbus => ../pvlogger-shared-modbus
