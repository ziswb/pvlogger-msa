package dataaccessor

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"pvlogger-shared-modbus/model"
	"sync"
	"testing"
)

func TestDataAccessorService_RetrieveEnergyTodaySoFar(t *testing.T) {
	var (
		mu           sync.Mutex
		requestCount int
		requestPath  string
	)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		mu.Lock()
		requestCount++
		requestPath = r.URL.Path
		mu.Unlock()
		w.WriteHeader(http.StatusOK)
		fmt.Fprint(w, "12345")
	}))
	defer server.Close()

	svc := &DataAccessorService{
		baseURL:    server.URL,
		httpClient: server.Client(),
	}

	t.Setenv("DA_READ_KW_TODAY_SO_FAR_PATH", "read-kw-today-so-far")

	md := &model.RegisterDefinition{
		Type:            "I32",
		Gain:            "HUNDRED",
		RegisterAddress: model.RegisterAddress{Value: 37121},
	}
	result, err := svc.retrieveEnergyTodaySoFar(md)
	if err != nil {
		t.Fatalf("retrieveEnergyTodaySoFar returned error: %v", err)
	}

	mu.Lock()
	defer mu.Unlock()
	if requestCount != 1 {
		t.Errorf("requestCount = %d, want 1 (endpoint is device-agnostic, must be called once)", requestCount)
	}
	if requestPath != "/read-kw-today-so-far/I32/37121" {
		t.Errorf("path = %q, want %q", requestPath, "/read-kw-today-so-far/I32/37121")
	}
	if result != 123.45 {
		t.Errorf("result = %f, want 123.45 (value divided by gain)", result)
	}
}
