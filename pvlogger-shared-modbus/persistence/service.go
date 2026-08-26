package persistence

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"pv-logger/logging"
	"time"
)

// ErrNotFound reports that the accessor answered normally and has no such
// value. Callers must be able to tell this apart from every other failure
// here: only a 404 can mean the run genuinely never stored the row, whereas a
// timeout, a refused connection or a 5xx says nothing at all about what the
// store contains. Conflating the two produced twelve days of misleading
// "the run must store the dongle aggregate block" errors during the incident
// in backlog 38, when the real cause was a 29.6 s query.
var ErrNotFound = errors.New("no such value in the data accessor")

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
	if serverResponse.StatusCode < 200 || serverResponse.StatusCode >= 300 {
		return fmt.Errorf("unexpected status code %d storing values", serverResponse.StatusCode)
	}
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
	if resp.StatusCode == http.StatusNotFound {
		return fmt.Errorf("%w: GET %s", ErrNotFound, getURL.String())
	}
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
