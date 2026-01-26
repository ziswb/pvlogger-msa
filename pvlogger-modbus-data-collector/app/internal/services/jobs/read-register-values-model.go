package jobs

import (
	"errors"
	"fmt"
	"net"
	"pvlogger-go-shared/device"
	"pvlogger-go-shared/logging"
	"pvlogger-modbus-data-collector/internal/config"
	"pvlogger-modbus-data-collector/internal/modbus"
	"pvlogger-modbus-data-collector/internal/modbus/model"
	"pvlogger-modbus-data-collector/internal/modbus/request"
	"pvlogger-modbus-data-collector/internal/services/persistence"
	"sync/atomic"
	"time"

	"github.com/google/uuid"
)

type DeviceIdToTypesMap map[model.UByte][]device.DeviceType

type ReadRegisterValuesService struct {
	jobId              model.JobId
	lastTransactionId  atomic.Int32
	categories         []model.Category
	deviceIdToTypesMap DeviceIdToTypesMap
	tcpHandler         *modbus.TcpHandler
	persistenceService *persistence.PersistingService
	ReadDevIdCode      model.UByte
	ObjectId           model.UByte
}

func NewReadRegisterValuesService(categories []model.Category) *ReadRegisterValuesService {
	return &ReadRegisterValuesService{
		jobId:              model.NewJobId(uuid.New().String()),
		lastTransactionId:  atomic.Int32{},
		categories:         categories,
		deviceIdToTypesMap: newDeviceIdToTypesMap(),
		tcpHandler:         modbus.NewTcpHandler(fmt.Sprintf("%s:%d", config.ModbusConfig().Host, config.ModbusConfig().Port), config.ModbusConfig().Timeout),
		persistenceService: persistence.NewPersistingService(config.DataAccessorCfg()),
	}
}

func NewReadDeviceIdentifiersService() *ReadRegisterValuesService {
	return &ReadRegisterValuesService{
		jobId:              model.NewJobId(uuid.New().String()),
		lastTransactionId:  atomic.Int32{},
		tcpHandler:         modbus.NewTcpHandler(fmt.Sprintf("%s:%d", config.ModbusConfig().Host, config.ModbusConfig().Port), config.ModbusConfig().Timeout),
		persistenceService: persistence.NewPersistingService(config.DataAccessorCfg()),
		ReadDevIdCode:      model.UByte(0x01),
		ObjectId:           model.UByte(0x00),
	}
}

func NewReadDeviceListService() *ReadRegisterValuesService {
	return &ReadRegisterValuesService{
		jobId:              model.NewJobId(uuid.New().String()),
		lastTransactionId:  atomic.Int32{},
		tcpHandler:         modbus.NewTcpHandler(fmt.Sprintf("%s:%d", config.ModbusConfig().Host, config.ModbusConfig().Port), config.ModbusConfig().Timeout),
		persistenceService: persistence.NewPersistingService(config.DataAccessorCfg()),
		ReadDevIdCode:      model.UByte(0x03),
		ObjectId:           model.UByte(0x87),
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
	var deviceIdToTypesMap = make(map[model.UByte][]device.DeviceType)
	appendIdToTypeEntries(deviceIdToTypesMap, config.DevIdConfig().MeterDeviceIds, device.MeterDeviceType)
	appendIdToTypeEntries(deviceIdToTypesMap, config.DevIdConfig().InverterDeviceIds, device.InverterDeviceType)
	return deviceIdToTypesMap
}

func appendIdToTypeEntries(deviceIdToTypesMap map[model.UByte][]device.DeviceType, devIds []model.UByte, deviceType device.DeviceType) {
	for _, id := range devIds {
		if len(deviceIdToTypesMap[id]) == 0 {
			deviceIdToTypesMap[id] = []device.DeviceType{deviceType}
		} else {
			deviceIdToTypesMap[id] = append(deviceIdToTypesMap[id], deviceType)
		}
	}
}
