module pvlogger-modbus-mock

go 1.27.4

require (
	pv-logger v0.0.0
	pvlogger-shared-modbus v0.0.0
)

replace pv-logger => ../pv-logger

replace pvlogger-shared-modbus => ../pvlogger-shared-modbus
