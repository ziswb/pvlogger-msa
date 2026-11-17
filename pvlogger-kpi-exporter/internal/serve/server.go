// Package serve is the cluster side of the KPI export (ADR-0021):
// a token-protected endpoint that returns the latest
// KPI row as the finished export file.
package serve

import (
	"crypto/subtle"
	"fmt"
	"net/http"
	"os"
	"strings"

	"pv-logger/logging"
	"pvlogger-kpi-exporter/internal/file"
	"pvlogger-kpi-exporter/internal/kpi"
)

const (
	ExportPath = "/v1/export/latest"
	HealthPath = "/healthz"
)

// FetchFunc returns the latest KPI row (kpi.FetchKpis in production).
type FetchFunc func() (*kpi.Values, error)

// NewHandler serves the export endpoint and an unauthenticated health check.
// The token is read from tokenFile on every request, so a rotated Secret takes
// effect without a restart; a missing or empty file rejects every request.
func NewHandler(tokenFile, filePrefix, keyPrefix string, fetch FetchFunc) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET "+HealthPath, func(w http.ResponseWriter, _ *http.Request) {
		_, _ = w.Write([]byte("ok\n"))
	})
	mux.HandleFunc("GET "+ExportPath, func(w http.ResponseWriter, r *http.Request) {
		if !authorized(r, tokenFile) {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		kpis, err := fetch()
		if err != nil {
			logging.Error("export: fetching the latest KPIs failed", "error", err)
			http.Error(w, "upstream unavailable", http.StatusBadGateway)
			return
		}
		content, err := file.Encode(kpi.MapToExportRecord(keyPrefix, kpis))
		if err != nil {
			logging.Error("export: encoding failed", "error", err)
			http.Error(w, "internal error", http.StatusInternalServerError)
			return
		}
		name := kpi.FileName(filePrefix, kpis.CreatedAt) + ".json"
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=%q", name))
		_, _ = w.Write(content)
		logging.Info("export: served", "file", name)
	})
	return mux
}

func authorized(r *http.Request, tokenFile string) bool {
	raw, err := os.ReadFile(tokenFile)
	if err != nil {
		logging.Error("export: cannot read token file, rejecting", "path", tokenFile, "error", err)
		return false
	}
	token := strings.TrimSpace(string(raw))
	if token == "" {
		logging.Error("export: token file is empty, rejecting", "path", tokenFile)
		return false
	}
	given, ok := strings.CutPrefix(r.Header.Get("Authorization"), "Bearer ")
	return ok && subtle.ConstantTimeCompare([]byte(given), []byte(token)) == 1
}
