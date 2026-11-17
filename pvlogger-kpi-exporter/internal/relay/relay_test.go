package relay

import (
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
	"time"

	"pvlogger-kpi-exporter/internal/kpi"
	"pvlogger-kpi-exporter/internal/serve"
)

func TestMain(m *testing.M) {
	_ = os.Setenv("TZ", "Europe/Zurich")
	os.Exit(m.Run())
}

// upstream runs the real cluster-side handler, so the test covers the contract
// between the two modes, not a hand-written imitation of it.
func upstream(t *testing.T, token string, values *kpi.Values) *httptest.Server {
	t.Helper()
	tf := filepath.Join(t.TempDir(), "token")
	if err := os.WriteFile(tf, []byte(token), 0600); err != nil {
		t.Fatal(err)
	}
	srv := httptest.NewServer(serve.NewHandler(tf, "pv", "site", func() (*kpi.Values, error) { return values, nil }))
	t.Cleanup(srv.Close)
	return srv
}

func call(h http.Handler) *httptest.ResponseRecorder {
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, ExportPath, nil))
	return rec
}

var client = &http.Client{Timeout: 5 * time.Second}

func TestRelay_WritesTodaysFileAndReturnsIt(t *testing.T) {
	values := &kpi.Values{CreatedAt: 1760994900123, SolarPower: 4.2, OwnConsumption: 1.5}
	up := upstream(t, "s3cret", values)
	dir := t.TempDir()

	rec := call(New(client, up.URL+serve.ExportPath, "s3cret", dir).Handler())

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", rec.Code, rec.Body)
	}
	written, err := os.ReadFile(filepath.Join(dir, "pv-2025-10-20_23-15-00.123.json"))
	if err != nil {
		t.Fatalf("expected today's file name: %v", err)
	}
	body, _ := io.ReadAll(rec.Body)
	if string(body) != string(written) {
		t.Errorf("response and file differ:\n%s\n%s", body, written)
	}
	entries, _ := os.ReadDir(dir)
	if len(entries) != 1 {
		t.Errorf("expected exactly one file (no temp leftovers), got %d", len(entries))
	}
}

func TestRelay_RunsTheCleanup(t *testing.T) {
	up := upstream(t, "s3cret", &kpi.Values{CreatedAt: 1760994900123})
	dir := t.TempDir()
	old := filepath.Join(dir, "pv-2025-10-18_10-00-00.000.json")
	if err := os.WriteFile(old, []byte("{}"), 0644); err != nil {
		t.Fatal(err)
	}
	past := time.Now().Add(-49 * time.Hour)
	if err := os.Chtimes(old, past, past); err != nil {
		t.Fatal(err)
	}

	if rec := call(New(client, up.URL+serve.ExportPath, "s3cret", dir).Handler()); rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rec.Code)
	}
	if _, err := os.Stat(old); !os.IsNotExist(err) {
		t.Error("expected the 49-hour-old file to be deleted")
	}
}

func TestRelay_WrongTokenWritesNothing(t *testing.T) {
	up := upstream(t, "s3cret", &kpi.Values{CreatedAt: 1760994900123})
	dir := t.TempDir()

	rec := call(New(client, up.URL+serve.ExportPath, "wrong", dir).Handler())

	if rec.Code != http.StatusBadGateway {
		t.Errorf("expected 502, got %d", rec.Code)
	}
	if entries, _ := os.ReadDir(dir); len(entries) != 0 {
		t.Errorf("expected no file, got %d", len(entries))
	}
}

func TestRelay_UnreachableUpstreamWritesNothing(t *testing.T) {
	dir := t.TempDir()
	rec := call(New(client, "http://127.0.0.1:1/v1/export/latest", "s3cret", dir).Handler())
	if rec.Code != http.StatusBadGateway {
		t.Errorf("expected 502, got %d", rec.Code)
	}
	if entries, _ := os.ReadDir(dir); len(entries) != 0 {
		t.Errorf("expected no file, got %d", len(entries))
	}
}

func TestRelay_RejectsUnsafeFileNames(t *testing.T) {
	for _, cd := range []string{
		``,
		`attachment`,
		`attachment; filename="../../etc/cron.d/x.json"`,
		`attachment; filename=".hidden.json"`,
		`attachment; filename="sub/dir.json"`,
		`attachment; filename="notjson.txt"`,
	} {
		up := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
			if cd != "" {
				w.Header().Set("Content-Disposition", cd)
			}
			_, _ = w.Write([]byte(`{"a":1}`))
		}))
		dir := t.TempDir()
		rec := call(New(client, up.URL, "t", dir).Handler())
		up.Close()
		if rec.Code != http.StatusBadGateway {
			t.Errorf("Content-Disposition %q: expected 502, got %d", cd, rec.Code)
		}
		if entries, _ := os.ReadDir(dir); len(entries) != 0 {
			t.Errorf("Content-Disposition %q: expected no file, got %d", cd, len(entries))
		}
	}
}

func TestRelay_RejectsNonJSONBody(t *testing.T) {
	up := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Disposition", `attachment; filename="pv-x.json"`)
		_, _ = w.Write([]byte("<html>login</html>"))
	}))
	defer up.Close()
	dir := t.TempDir()
	if rec := call(New(client, up.URL, "t", dir).Handler()); rec.Code != http.StatusBadGateway {
		t.Errorf("expected 502, got %d", rec.Code)
	}
	if entries, _ := os.ReadDir(dir); len(entries) != 0 {
		t.Errorf("expected no file, got %d", len(entries))
	}
}
