package auth

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"

	"pvlogger-fusionsolar/internal/config"
	"pvlogger-fusionsolar/internal/model"
)

func TestCookieValue_Found(t *testing.T) {
	cookies := []*http.Cookie{
		{Name: "session", Value: "abc123"},
		{Name: "XSRF-TOKEN", Value: "token-value"},
	}
	result := CookieValue(cookies, "XSRF-TOKEN")
	if result != "token-value" {
		t.Errorf("expected 'token-value', got '%s'", result)
	}
}

func TestCookieValue_NotFound(t *testing.T) {
	cookies := []*http.Cookie{
		{Name: "session", Value: "abc123"},
	}
	result := CookieValue(cookies, "XSRF-TOKEN")
	if result != "" {
		t.Errorf("expected empty string, got '%s'", result)
	}
}

func TestCookieValue_EmptyCookies(t *testing.T) {
	result := CookieValue([]*http.Cookie{}, "XSRF-TOKEN")
	if result != "" {
		t.Errorf("expected empty string, got '%s'", result)
	}
}

func TestCookieValue_NilCookies(t *testing.T) {
	result := CookieValue(nil, "XSRF-TOKEN")
	if result != "" {
		t.Errorf("expected empty string, got '%s'", result)
	}
}

func TestCookieValue_FirstMatchReturned(t *testing.T) {
	cookies := []*http.Cookie{
		{Name: "token", Value: "first"},
		{Name: "token", Value: "second"},
	}
	result := CookieValue(cookies, "token")
	if result != "first" {
		t.Errorf("expected 'first', got '%s'", result)
	}
}

func TestCookieValue_EmptyValue(t *testing.T) {
	cookies := []*http.Cookie{
		{Name: "XSRF-TOKEN", Value: ""},
	}
	result := CookieValue(cookies, "XSRF-TOKEN")
	if result != "" {
		t.Errorf("expected empty string, got '%s'", result)
	}
}

// A FusionSolar endpoint that never answers must fail the login instead of
// hanging the CronJob (security finding 41).
func TestAuthenticate_TimesOutOnStalledServer(t *testing.T) {
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
	cfg := &config.Config{Fs: config.FusionSolarConfig{Protocol: u.Scheme, Host: u.Host}}

	start := time.Now()
	_, err := Authenticate(cfg)
	if err == nil {
		t.Fatal("expected a timeout error, got nil")
	}
	if elapsed := time.Since(start); elapsed > 2*time.Second {
		t.Errorf("expected to give up after ~100ms, took %s", elapsed)
	}
}
