package services

import (
	"pvlogger-go-shared/logging"
	"pvlogger-modbus-data-collector/internal/modbus/model"
	"pvlogger-modbus-data-collector/internal/services/jobs"
)

func FetchModbusStaticRegisterValues() {
	logging.Info("fetching modbus static register values")
	jobs.NewReadRegisterValuesService([]model.Category{model.StaticCategory, model.ConfigCategory}).ExecuteFetchRegisterValuesJob()
}

func FetchDeviceIdentifiers() {
	logging.Info("fetching modbus device identifiers")
	jobs.NewReadDeviceIdentifiersService().ExecuteFetchDeviceIdentifiersJob()
}

func FetchDeviceList() {
	logging.Info("fetching modbus device list")
	jobs.NewReadDeviceListService().ExecuteFetchDeviceIdentifiersJob()
}
