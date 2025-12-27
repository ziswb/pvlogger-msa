package services

import (
	"log"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"

	"github.com/google/uuid"
)

const (
	StaticCategory = "STATIC"
	ConfigCategory = "CONFIG"
)

func FetchModbusStaticRegisterValues() *model.JobId {
	var deviceIdToTypesMap = NewDeviceIdToTypesMap()
	jobId := model.NewJobId(uuid.New().String())
	log.Default().Println("Fetching Modbus Static Register Values. Job ID:", jobId)
	for deviceId, deviceTypes := range deviceIdToTypesMap {
		fetchRegisterValues(jobId, deviceId, deviceTypes)
	}
	return &jobId
}
