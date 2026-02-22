package dataaccessor

import (
	"fmt"
	"net/http"
	"pv-full-data-collector/internal/config"
	"pv-full-data-collector/internal/services/masterdata"
	"time"
)

type DataAccessorService struct {
	baseURL           string
	httpClient        *http.Client
	masterDataService *masterdata.MasterDataService
}

func NewDataAccessorService() *DataAccessorService {
	return &DataAccessorService{
		baseURL: fmt.Sprintf("%s://%s:%d", config.DataAccessorCfg().Protocol, config.DataAccessorCfg().Host, config.DataAccessorCfg().Port),
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
		masterDataService: masterdata.NewMasterDataService(),
	}
}
