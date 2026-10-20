package fusionsolar

import (
	"context"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"

	"pvlogger-fusionsolar/internal/auth"
	"pvlogger-fusionsolar/internal/config"
	"pvlogger-fusionsolar/internal/model"
)

// The KPI calls must give up on a FusionSolar endpoint that never answers
// instead of hanging the CronJob (security finding 41).
func TestNewClient_TimesOutOnStalledServer(t *testing.T) {
	stop := make(chan struct{})
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		select {
		case <-r.Context().Done():
		case <-stop:
		}
	}))
	defer srv.Close()
	defer close(stop) // runs first: lets the stalled handler return
	defer func(d time.Duration) { model.RequestTimeout = d }(model.RequestTimeout)
	model.RequestTimeout = 100 * time.Millisecond

	u, _ := url.Parse(srv.URL)
	f := &FusionSolarAccess{
		cfg:   &config.Config{Fs: config.FusionSolarConfig{Protocol: u.Scheme, Host: u.Host}},
		token: auth.Token{Token: "t"},
	}
	client, err := f.newClient()
	if err != nil {
		t.Fatal(err)
	}

	start := time.Now()
	_, err = client.KpiStationDayWithResponse(context.Background(), model.KpiStationDayJSONRequestBody{})
	if err == nil {
		t.Fatal("expected a timeout error, got nil")
	}
	if elapsed := time.Since(start); elapsed > 2*time.Second {
		t.Errorf("expected to give up after ~100ms, took %s", elapsed)
	}
}
