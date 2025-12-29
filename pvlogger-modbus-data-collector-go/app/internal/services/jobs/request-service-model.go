package jobs

import (
	"fmt"
	"log"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/modbus"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/modbus/request"
	"pvlogger-modbus-data-collector-go/internal/services/persistence"
	"sync/atomic"
	"time"

	"github.com/google/uuid"
)

type DeviceIdToTypesMap map[model.UByte][]model.DeviceType

type RequestService struct {
	jobId              model.JobId
	lastTransactionId  atomic.Int32
	categories         []model.Category
	deviceIdToTypesMap DeviceIdToTypesMap
	tcpHandler         *modbus.TcpHandler
	persistenceService *persistence.PersistingService
}

func NewRequestService(categories []model.Category) *RequestService {
	return &RequestService{
		jobId:              model.NewJobId(uuid.New().String()),
		lastTransactionId:  atomic.Int32{},
		categories:         categories,
		deviceIdToTypesMap: newDeviceIdToTypesMap(),
		tcpHandler:         modbus.NewTcpHandler(fmt.Sprintf("%s:%d", config.ModbusConfig().Host, config.ModbusConfig().Port), config.ModbusConfig().Timeout),
		persistenceService: persistence.NewPersistingService(config.DataAccessorCfg()),
	}
}

type RequestServiceInterface interface {
	JobId() model.JobId
	NextRequestId() model.UShort
	Close()
	Connect()
}

func (s *RequestService) JobId() *model.JobId {
	return &s.jobId
}

func (s *RequestService) newRegisterValueRequest(deviceId model.UByte, registerDefinitions []model.RegisterDefinition) *request.ReadHoldingRegistersValueRequest {
	return &request.ReadHoldingRegistersValueRequest{
		JobId:               s.jobId,
		RequestTime:         time.Now(),
		RequestId:           model.NewRequestId(s.NextRequestId()),
		LogicDeviceId:       deviceId,
		RegisterDefinitions: registerDefinitions,
	}
}

func (s *RequestService) Close() {
	log.Default().Println("Closing Modbus connection...")
	err := s.tcpHandler.Close()
	if err != nil {
		log.Default().Println("Error closing Modbus connection:", err)
	}
}

func (s *RequestService) Connect() {
	log.Default().Println("Connecting to Modbus device...")
	err := s.tcpHandler.Connect()
	if err != nil {
		log.Default().Println("Error connecting to Modbus device:", err)
	}
}

func (s *RequestService) NextRequestId() model.UShort {
	return model.UShort(s.lastTransactionId.Add(1))
}

func newDeviceIdToTypesMap() DeviceIdToTypesMap {
	var deviceIdToTypesMap = make(map[model.UByte][]model.DeviceType)
	appendIdToTypeEntries(deviceIdToTypesMap, config.DevIdConfig().MeterDeviceIds, model.MeterDeviceType)
	appendIdToTypeEntries(deviceIdToTypesMap, config.DevIdConfig().InverterDeviceIds, model.InverterDeviceType)
	return deviceIdToTypesMap
}

func appendIdToTypeEntries(deviceIdToTypesMap map[model.UByte][]model.DeviceType, devIds []model.UByte, deviceType model.DeviceType) {
	for _, id := range devIds {
		if len(deviceIdToTypesMap[id]) == 0 {
			deviceIdToTypesMap[id] = []model.DeviceType{deviceType}
		} else {
			deviceIdToTypesMap[id] = append(deviceIdToTypesMap[id], deviceType)
		}
	}
}
