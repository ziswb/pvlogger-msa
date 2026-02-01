package auth

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"pvlogger-fusionsolar-auth/internal/config"

	"pvlogger-go-shared/logging"
)

type LoginRequest struct {
	SystemCode string `json:"systemCode"`
	UserName   string `json:"userName"`
}

type LoginResponse struct {
	FailCode int    `json:"failCode"`
	Message  string `json:"message,omitempty"`
	Success  bool   `json:"success,omitempty"`
}

func Authenticate(cfg *config.Config) (string, error) {
	logging.Info("authenticating with FusionSolar API")

	loginURL := cfg.FsUrl() + "/login"
	reqBody := LoginRequest{
		SystemCode: cfg.FusionSolar.Password,
		UserName:   cfg.FusionSolar.Username,
	}

	bodyBytes, err := json.Marshal(reqBody)
	if err != nil {
		return "", fmt.Errorf("failed to marshal login request: %w", err)
	}

	req, err := http.NewRequestWithContext(context.Background(), http.MethodPost, loginURL, nil)
	if err != nil {
		return "", fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Body = io.NopCloser(jsonReader(bodyBytes))
	req.ContentLength = int64(len(bodyBytes))

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return "", fmt.Errorf("login request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("login failed with status: %d", resp.StatusCode)
	}

	responseBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("failed to read response body: %w", err)
	}

	var loginResp LoginResponse
	if err := json.Unmarshal(responseBody, &loginResp); err != nil {
		return "", fmt.Errorf("failed to unmarshal response: %w", err)
	}

	if loginResp.FailCode != 0 {
		return "", fmt.Errorf("login failed: %s (code: %d)", loginResp.Message, loginResp.FailCode)
	}

	token := cookieValue(resp.Cookies(), "XSRF-TOKEN")
	if token == "" {
		return "", fmt.Errorf("XSRF-TOKEN not found in response cookies")
	}

	logging.Info("authentication successful")
	return token, nil
}

func cookieValue(cookies []*http.Cookie, name string) string {
	for _, c := range cookies {
		if c.Name == name {
			return c.Value
		}
	}
	return ""
}

type jsonReaderType []byte

func jsonReader(data []byte) *jsonReaderType {
	r := jsonReaderType(data)
	return &r
}

func (r *jsonReaderType) Read(p []byte) (n int, err error) {
	if len(*r) == 0 {
		return 0, io.EOF
	}
	n = copy(p, *r)
	*r = (*r)[n:]
	return n, nil
}
