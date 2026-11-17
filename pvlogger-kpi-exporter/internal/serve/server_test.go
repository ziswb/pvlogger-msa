package serve

import (
	"errors"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"

	"pvlogger-kpi-exporter/internal/file"
	"pvlogger-kpi-exporter/internal/kpi"
)

func TestMain(m *testing.M) {
	_ = os.Setenv("TZ", "Europe/Zurich")
	os.Exit(m.Run())
}

var sample = &kpi.Values{CreatedAt: 1760994900123, SolarPower: 4.2, OwnConsumption: 1.5}

func tokenFile(t *testing.T, content string) string {
	t.Helper()
	p := filepath.Join(t.TempDir(), "token")
	if err := os.WriteFile(p, []byte(content), 0600); err != nil {
		t.Fatal(err)
	}
	return p
}

func get(h http.Handler, path, auth string) *httptest.ResponseRecorder {
	req := httptest.NewRequest(http.MethodGet, path, nil)
	if auth != "" {
		req.Header.Set("Authorization", auth)
	}
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	return rec
}

func okFetch() (*kpi.Values, error) { return sample, nil }

func TestExport_ReturnsTodaysFileContentAndName(t *testing.T) {
	h := NewHandler(tokenFile(t, "s3cret\n"), "pv", "site", okFetch)

	rec := get(h, ExportPath, "Bearer s3cret")

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rec.Code)
	}
	want, _ := file.Encode(kpi.MapToExportRecord("site", sample))
	if got, _ := io.ReadAll(rec.Body); string(got) != string(want) {
		t.Errorf("body differs from what the CronJob writes:\n got %s\nwant %s", got, want)
	}
	wantName := `attachment; filename="pv-2025-10-20_23-15-00.123.json"`
	if cd := rec.Header().Get("Content-Disposition"); cd != wantName {
		t.Errorf("Content-Disposition: got %q, want %q", cd, wantName)
	}
	if ct := rec.Header().Get("Content-Type"); ct != "application/json" {
		t.Errorf("Content-Type: got %q", ct)
	}
}

func TestExport_RejectsMissingOrWrongToken(t *testing.T) {
	h := NewHandler(tokenFile(t, "s3cret"), "pv", "site", okFetch)
	for _, auth := range []string{"", "Bearer wrong", "s3cret", "Basic s3cret", "Bearer s3cret2"} {
		if rec := get(h, ExportPath, auth); rec.Code != http.StatusUnauthorized {
			t.Errorf("Authorization %q: expected 401, got %d", auth, rec.Code)
		}
	}
}

func TestExport_EmptyOrMissingTokenFileRejectsEverything(t *testing.T) {
	for name, path := range map[string]string{
		"empty":   tokenFile(t, "  \n"),
		"missing": filepath.Join(t.TempDir(), "nope"),
	} {
		h := NewHandler(path, "pv", "site", okFetch)
		if rec := get(h, ExportPath, "Bearer "); rec.Code != http.StatusUnauthorized {
			t.Errorf("%s token file: expected 401, got %d", name, rec.Code)
		}
	}
}

func TestExport_ReadsTheTokenOnEveryRequest(t *testing.T) {
	path := tokenFile(t, "old")
	h := NewHandler(path, "pv", "site", okFetch)
	if err := os.WriteFile(path, []byte("new"), 0600); err != nil {
		t.Fatal(err)
	}
	if rec := get(h, ExportPath, "Bearer old"); rec.Code != http.StatusUnauthorized {
		t.Errorf("old token after rotation: expected 401, got %d", rec.Code)
	}
	if rec := get(h, ExportPath, "Bearer new"); rec.Code != http.StatusOK {
		t.Errorf("new token after rotation: expected 200, got %d", rec.Code)
	}
}

func TestExport_UpstreamFailureIs502(t *testing.T) {
	h := NewHandler(tokenFile(t, "s3cret"), "pv", "site", func() (*kpi.Values, error) {
		return nil, errors.New("data accessor down")
	})
	if rec := get(h, ExportPath, "Bearer s3cret"); rec.Code != http.StatusBadGateway {
		t.Errorf("expected 502, got %d", rec.Code)
	}
}

func TestHealth_NeedsNoToken(t *testing.T) {
	h := NewHandler(tokenFile(t, "s3cret"), "pv", "site", okFetch)
	if rec := get(h, HealthPath, ""); rec.Code != http.StatusOK {
		t.Errorf("expected 200, got %d", rec.Code)
	}
}

func TestExport_OnlyGet(t *testing.T) {
	h := NewHandler(tokenFile(t, "s3cret"), "pv", "site", okFetch)
	req := httptest.NewRequest(http.MethodPost, ExportPath, nil)
	req.Header.Set("Authorization", "Bearer s3cret")
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusMethodNotAllowed {
		t.Errorf("expected 405, got %d", rec.Code)
	}
}
