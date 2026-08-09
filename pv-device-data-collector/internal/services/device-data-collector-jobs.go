package services

import (
	"pv-logger/logging"
	"pvlogger-shared-modbus/collector"
	"pvlogger-shared-modbus/model"
)

func FetchModbusStaticRegisterValues() error {
	logging.Info("fetching modbus static register values")
	return collector.NewReadRegisterValuesService([]model.Category{model.StaticCategory, model.ConfigCategory}).ExecuteFetchRegisterValuesJob()
}

func FetchDeviceIdentifiers() error {
	logging.Info("fetching modbus device identifiers")
	return collector.NewReadDeviceIdentifiersService().ExecuteFetchDeviceIdentifiersJob()
}

func FetchDeviceList() error {
	logging.Info("fetching modbus device list")
	return collector.NewReadDeviceListService().ExecuteFetchDeviceIdentifiersJob()
}
