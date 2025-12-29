package services

import (
	"log"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/services/jobs"
)

func FetchModbusDynamicRegisterValues() *model.JobId {
	log.Default().Println("Fetching Modbus Dynamic Register Values")
	service := jobs.NewRequestService([]model.Category{model.DynamicCategory, model.AlarmCategory})
	service.ExecuteFetchRegisterValuesJob()
	return service.JobId()
}
