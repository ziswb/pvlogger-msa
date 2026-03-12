package persistence

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"pv-logger/logging"
	"time"
)

type PersistingService struct {
	baseURL    string
	basePath   string
	httpClient *http.Client
}

func NewPersistingService(host string, port int, protocol string, basePath string) *PersistingService {
	return &PersistingService{
		baseURL:  fmt.Sprintf("%s://%s:%d", protocol, host, port),
		basePath: basePath,
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}

func (s *PersistingService) StoreValues(values any, storagePath string) {
	logging.Info("storing values")
	postURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, s.basePath, storagePath))
	if err != nil {
		logging.Fatal("failed to parse URL", "error", err)
	}
	logging.Debug("POST request", "url", postURL.String())
	var b bytes.Buffer
	err = json.NewEncoder(&b).Encode(values)
	if err != nil {
		logging.Fatal("failed to encode object", "error", err)
	}
	request, err := http.NewRequest("POST", postURL.String(), &b)
	if err != nil {
		logging.Fatal("failed to create request", "error", err)
	}
	request.Header.Set("Content-Type", "application/json")
	serverResponse, err := s.httpClient.Do(request)
	if err != nil {
		logging.Fatal("failed to send request", "error", err)
	}
	defer CloseBody(serverResponse)
	logging.Info("values stored", "status", serverResponse.Status)
}

func (s *PersistingService) GetJSON(path string, result any) {
	getURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, s.basePath, path))
	if err != nil {
		logging.Fatal("failed to parse URL", "error", err)
	}
	logging.Debug("GET request", "url", getURL.String())
	resp, err := s.httpClient.Get(getURL.String())
	if err != nil {
		logging.Fatal("failed to send GET request", "error", err)
	}
	defer CloseBody(resp)
	if resp.StatusCode != http.StatusOK {
		logging.Fatal("unexpected status code", "status_code", resp.StatusCode)
	}
	if err := json.NewDecoder(resp.Body).Decode(result); err != nil {
		logging.Fatal("failed to decode response", "error", err)
	}
}

func CloseBody(resp *http.Response) {
	if resp != nil && resp.Body != nil {
		logging.Debug("closing response body")
		if err := resp.Body.Close(); err != nil {
			logging.Error("failed to close response body", "error", err)
		}
	}
}

// Deprecated: BaseURL returns the base URL for backwards compatibility.
// Prefer using StoreValues and GetJSON directly.
func (s *PersistingService) BaseURL() string {
	return s.baseURL
}

// Deprecated: HTTPClient returns the http.Client for backwards compatibility.
func (s *PersistingService) HTTPClient() *http.Client {
	return s.httpClient
}

// BasePath returns the base API path.
func (s *PersistingService) BasePath() string {
	return s.basePath
}
