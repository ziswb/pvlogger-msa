package dataaccessor

import (
	"fmt"
	"net/http"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/masterdata"
	"time"
)

type DataAccessorService struct {
	baseURL           string
	httpClient        *http.Client
	masterDataService *masterdata.MasterDataService
}

func NewDataAccessorService() *DataAccessorService {
	return &DataAccessorService{
		baseURL: fmt.Sprintf("%s://%s:%d", sharedconfig.DataAccessorCfg().Protocol, sharedconfig.DataAccessorCfg().Host, sharedconfig.DataAccessorCfg().Port),
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
		masterDataService: masterdata.NewMasterDataService(),
	}
}
