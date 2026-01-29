package masterdata

import (
	"fmt"
	"net/http"
	"pvlogger-modbus-data-collector/internal/config"
	"pvlogger-modbus-data-collector/internal/modbus/model"
	"time"
)

const (
	paramDevTypes       = "deviceTypes"
	paramCategories     = "categories"
	paramValueSeparator = ";"
)

var (
	smartMeterActivePowerRegisterAddress = model.RegisterAddress{Value: 37113} // -> W
	inverterActivePowerRegisterAddress   = model.RegisterAddress{Value: 32080} // -> kW
)
var (
	smartMeterExportedEnergyRegisterAddress = model.RegisterAddress{Value: 37119} // -> kWh
	smartMeterImportedEnergyRegisterAddress = model.RegisterAddress{Value: 37121} // -> kWh
	inverterDailyEnergyYieldRegisterAddress = model.RegisterAddress{Value: 32114} // -> kWh
)

type MasterDataService struct {
	baseURL    string
	httpClient *http.Client
}

func NewMasterDataService() *MasterDataService {
	return &MasterDataService{
		baseURL: fmt.Sprintf("%s://%s:%d", config.MdConfig().Protocol, config.MdConfig().Host, config.MdConfig().Port),
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}
