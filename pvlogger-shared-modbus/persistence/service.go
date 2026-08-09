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

func (s *PersistingService) StoreValues(values any, storagePath string) error {
	logging.Info("storing values")
	postURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, s.basePath, storagePath))
	if err != nil {
		return fmt.Errorf("failed to parse URL: %w", err)
	}
	logging.Debug("POST request", "url", postURL.String())
	var b bytes.Buffer
	err = json.NewEncoder(&b).Encode(values)
	if err != nil {
		return fmt.Errorf("failed to encode object: %w", err)
	}
	request, err := http.NewRequest("POST", postURL.String(), &b)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}
	request.Header.Set("Content-Type", "application/json")
	serverResponse, err := s.httpClient.Do(request)
	if err != nil {
		return fmt.Errorf("failed to send request: %w", err)
	}
	defer CloseBody(serverResponse)
	logging.Info("values stored", "status", serverResponse.Status)
	return nil
}

func (s *PersistingService) GetJSON(path string, result any) error {
	getURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, s.basePath, path))
	if err != nil {
		return fmt.Errorf("failed to parse URL: %w", err)
	}
	logging.Debug("GET request", "url", getURL.String())
	resp, err := s.httpClient.Get(getURL.String())
	if err != nil {
		return fmt.Errorf("failed to send GET request: %w", err)
	}
	defer CloseBody(resp)
	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("unexpected status code %d", resp.StatusCode)
	}
	if err := json.NewDecoder(resp.Body).Decode(result); err != nil {
		return fmt.Errorf("failed to decode response: %w", err)
	}
	return nil
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
