package jobs

import (
	"errors"
	"fmt"
	"net"
	"pv-full-data-collector/internal/config"
	modbus "pvlogger-shared-modbus"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/request"
	"pv-full-data-collector/internal/services/persistence"
	"pvlogger-go-shared/logging"
	"sync/atomic"
	"time"

	"github.com/google/uuid"
)

type DeviceIdToTypesMap map[model.UByte][]model.DeviceType

type ReadRegisterValuesService struct {
	jobId              model.JobId
	lastTransactionId  atomic.Int32
	categories         []model.Category
	deviceIdToTypesMap DeviceIdToTypesMap
	tcpHandler         *modbus.TcpHandler
	persistenceService *persistence.PersistingService
}

func NewReadRegisterValuesService(categories []model.Category) *ReadRegisterValuesService {
	return &ReadRegisterValuesService{
		jobId:              model.NewJobId(uuid.New().String()),
		lastTransactionId:  atomic.Int32{},
		categories:         categories,
		deviceIdToTypesMap: newDeviceIdToTypesMap(),
		tcpHandler:         modbus.NewTcpHandler(fmt.Sprintf("%s:%d", config.ModbusConfig().Host, config.ModbusConfig().Port), config.ModbusConfig().Timeout, config.ModbusConfig().ConnectionSleep),
		persistenceService: persistence.NewPersistingService(config.DataAccessorCfg()),
	}
}

func (s *ReadRegisterValuesService) JobId() *model.JobId {
	return &s.jobId
}

func (s *ReadRegisterValuesService) NewReadRegisterValuesRequest(deviceId model.UByte, registerDefinitions []model.RegisterDefinition) *request.ReadHoldingRegistersValueRequest {
	return &request.ReadHoldingRegistersValueRequest{
		JobId:               s.jobId,
		RequestTime:         time.Now(),
		RequestId:           model.NewRequestId(s.NextRequestId()),
		LogicDeviceId:       deviceId,
		RegisterDefinitions: registerDefinitions,
	}
}

func (s *ReadRegisterValuesService) Close() {
	logging.Info("closing modbus connection")
	err := s.tcpHandler.Close()
	if errors.Is(err, net.ErrClosed) {
		logging.Warn("modbus connection already closed")
		return
	}
	if err != nil {
		logging.Error("error closing modbus connection", "error", err)
	}
}

func (s *ReadRegisterValuesService) Connect() {
	logging.Info("connecting to modbus device")
	err := s.tcpHandler.Connect()
	if err != nil {
		logging.Fatal("error connecting to modbus device", "error", err)
	}
}

func (s *ReadRegisterValuesService) NextRequestId() model.UShort {
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
