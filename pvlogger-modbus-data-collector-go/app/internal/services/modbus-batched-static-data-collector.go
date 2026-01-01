package services

import (
	"pvlogger-modbus-data-collector-go/internal/logging"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/services/jobs"
)

func FetchModbusStaticRegisterValues() {
	logging.Info("fetching modbus static register values")
	jobs.NewRequestService([]model.Category{model.StaticCategory, model.ConfigCategory}).ExecuteFetchRegisterValuesJob()
}
