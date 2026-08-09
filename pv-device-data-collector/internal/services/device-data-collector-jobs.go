package services

import (
	"pv-logger/logging"
	"pvlogger-shared-modbus/model"
	"pv-device-data-collector/internal/services/jobs"
)

func FetchModbusStaticRegisterValues() error {
	logging.Info("fetching modbus static register values")
	return jobs.NewReadRegisterValuesService([]model.Category{model.StaticCategory, model.ConfigCategory}).ExecuteFetchRegisterValuesJob()
}

func FetchDeviceIdentifiers() error {
	logging.Info("fetching modbus device identifiers")
	return jobs.NewReadDeviceIdentifiersService().ExecuteFetchDeviceIdentifiersJob()
}

func FetchDeviceList() error {
	logging.Info("fetching modbus device list")
	return jobs.NewReadDeviceListService().ExecuteFetchDeviceIdentifiersJob()
}
