package services

import (
	"log"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/services/jobs"
)

func FetchModbusStaticRegisterValues() {
	log.Default().Println("Fetching Modbus Static Register Values")
	jobs.NewRequestService([]model.Category{model.StaticCategory, model.ConfigCategory}).ExecuteFetchRegisterValuesJob()
}
