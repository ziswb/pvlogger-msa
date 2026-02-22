package persistence

import (
	"fmt"
	"net/http"
	"pv-dynamic-data-collector/internal/config"
	"time"
)

type PersistingService struct {
	baseURL    string
	httpClient *http.Client
}

func NewPersistingService(config *config.DataAccessorConfig) *PersistingService {
	return &PersistingService{
		baseURL: fmt.Sprintf("%s://%s:%d", config.Protocol, config.Host, config.Port),
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}
