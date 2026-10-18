package fusionsolar

import (
	"net/http"
	"strings"
	"testing"
)

func newHTTPResponse(code int, contentType string) *http.Response {
	h := http.Header{}
	if contentType != "" {
		h.Set("Content-Type", contentType)
	}
	return &http.Response{StatusCode: code, Status: http.StatusText(code), Header: h}
}

func TestRequireJSON200_Decoded(t *testing.T) {
	err := requireJSON200("getStationRealKpi", true, newHTTPResponse(200, "application/json"), []byte(`{}`))
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
}

func TestRequireJSON200_ServerError(t *testing.T) {
	err := requireJSON200("getDevRealKpi", false, newHTTPResponse(503, "text/html"), []byte("<html>maintenance</html>"))
	if err == nil {
		t.Fatal("expected an error for a 503 reply")
	}
	for _, want := range []string{"getDevRealKpi", "Service Unavailable", "text/html", "maintenance"} {
		if !strings.Contains(err.Error(), want) {
			t.Errorf("error %q does not mention %q", err, want)
		}
	}
}

func TestRequireJSON200_NoHTTPResponse(t *testing.T) {
	err := requireJSON200("getDevRealKpi", false, nil, nil)
	if err == nil || !strings.Contains(err.Error(), "no HTTP response") {
		t.Fatalf("expected a 'no HTTP response' error, got %v", err)
	}
}

func TestRequireJSON200_TruncatesLongBody(t *testing.T) {
	body := []byte(strings.Repeat("x", maxErrorBodyBytes+100))
	err := requireJSON200("getDevRealKpi", false, newHTTPResponse(200, "text/plain"), body)
	if err == nil {
		t.Fatal("expected an error for a non-JSON reply")
	}
	if strings.Contains(err.Error(), strings.Repeat("x", maxErrorBodyBytes+1)) {
		t.Error("body was not truncated")
	}
	if !strings.Contains(err.Error(), "612 bytes total") {
		t.Errorf("error %q does not report the full body size", err)
	}
}
