package collector

import (
	"fmt"
	"pv-logger/logging"
	modbus "pvlogger-shared-modbus"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/persistence"
	"pvlogger-shared-modbus/request"
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
	ReadDevIdCode      model.UByte
	ObjectId           model.UByte
}

func NewReadRegisterValuesService(categories []model.Category) *ReadRegisterValuesService {
	daCfg := sharedconfig.DataAccessorCfg()
	mbCfg := sharedconfig.ModbusConfig()
	return &ReadRegisterValuesService{
		jobId:              model.NewJobId(uuid.New().String()),
		lastTransactionId:  atomic.Int32{},
		categories:         categories,
		deviceIdToTypesMap: newDeviceIdToTypesMap(),
		tcpHandler:         modbus.NewTcpHandler(fmt.Sprintf("%s:%d", mbCfg.Host, mbCfg.Port), mbCfg.Timeout, mbCfg.ConnectionSleep),
		persistenceService: persistence.NewPersistingService(daCfg.Host, daCfg.Port, daCfg.Protocol, sharedconfig.DataAccessorBasePath),
	}
}

func NewReadDeviceIdentifiersService() *ReadRegisterValuesService {
	daCfg := sharedconfig.DataAccessorCfg()
	mbCfg := sharedconfig.ModbusConfig()
	return &ReadRegisterValuesService{
		jobId:              model.NewJobId(uuid.New().String()),
		lastTransactionId:  atomic.Int32{},
		tcpHandler:         modbus.NewTcpHandler(fmt.Sprintf("%s:%d", mbCfg.Host, mbCfg.Port), mbCfg.Timeout, mbCfg.ConnectionSleep),
		persistenceService: persistence.NewPersistingService(daCfg.Host, daCfg.Port, daCfg.Protocol, sharedconfig.DataAccessorBasePath),
		ReadDevIdCode:      model.UByte(0x01),
		ObjectId:           model.UByte(0x00),
	}
}

func NewReadDeviceListService() *ReadRegisterValuesService {
	daCfg := sharedconfig.DataAccessorCfg()
	mbCfg := sharedconfig.ModbusConfig()
	return &ReadRegisterValuesService{
		jobId:              model.NewJobId(uuid.New().String()),
		lastTransactionId:  atomic.Int32{},
		tcpHandler:         modbus.NewTcpHandler(fmt.Sprintf("%s:%d", mbCfg.Host, mbCfg.Port), mbCfg.Timeout, mbCfg.ConnectionSleep),
		persistenceService: persistence.NewPersistingService(daCfg.Host, daCfg.Port, daCfg.Protocol, sharedconfig.DataAccessorBasePath),
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
	if err := s.tcpHandler.Close(); err != nil {
		logging.Error("error closing modbus connection", "error", err)
	}
}

func (s *ReadRegisterValuesService) Connect() error {
	logging.Info("connecting to modbus device")
	err := s.tcpHandler.Connect()
	if err != nil {
		return fmt.Errorf("error connecting to modbus device: %w", err)
	}
	return nil
}

func (s *ReadRegisterValuesService) NextRequestId() model.UShort {
	return model.UShort(s.lastTransactionId.Add(1))
}

func newDeviceIdToTypesMap() DeviceIdToTypesMap {
	var deviceIdToTypesMap = make(map[model.UByte][]model.DeviceType)
	appendIdToTypeEntries(deviceIdToTypesMap, sharedconfig.DevIdConfig().MeterDeviceIds, model.MeterDeviceType)
	appendIdToTypeEntries(deviceIdToTypesMap, sharedconfig.DevIdConfig().InverterDeviceIds, model.InverterDeviceType)
	// The dongle's own logical device (unit 100) serves the aggregate
	// live-power block (backlog 35). Its catalog rows are DYNAMIC only, so
	// jobs sweeping other categories fetch an empty list for it and skip.
	appendIdToTypeEntries(deviceIdToTypesMap, []model.UByte{sharedconfig.DevIdConfig().DongleDeviceId}, model.DongleDeviceType)
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
