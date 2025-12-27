package services

import (
	"log"
	"pvlogger-modbus-data-collector-go/internal/modbus"

	"github.com/google/uuid"
)

const (
	DynamicCategory = "DYNAMIC"
	AlarmCategory   = "ALARM"
)

func FetchModbusDynamicRegisterValues() *modbus.JobId {
	var deviceIdToTypesMap = NewDeviceIdToTypesMap()
	jobId := modbus.NewJobId(uuid.New().String())
	log.Default().Println("Fetching Modbus Dynamic Register Values. Job ID:", jobId)
	for deviceId, deviceTypes := range deviceIdToTypesMap {
		fetchRegisterValues(jobId, deviceId, deviceTypes)
	}
	return &jobId
}
