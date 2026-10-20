// Package relay is the container on the receiver's server (infrastructure
// ADR-0024, pvlogger-msa ADR-0021). Each call to /export fetches the latest
// export file from the cluster, writes it into the export directory under the
// name the cluster chose, runs the same 48-hour cleanup as the CronJob, and
// returns the JSON. It writes what it receives byte for byte: format, file
// prefix and formulas live in the cluster, so changing them never needs a new
// relay on his server.
package relay

import (
	"encoding/json"
	"fmt"
	"io"
	"mime"
	"net/http"
	"regexp"
	"sync"

	"pv-logger/logging"
	"pvlogger-kpi-exporter/internal/file"
)

const (
	ExportPath = "/export"
	HealthPath = "/healthz"

	maxBody = 1 << 20 // a real export file is well under 1 KiB
)

// A plain file name with a .json extension: no directories, nothing hidden.
var safeName = regexp.MustCompile(`^[A-Za-z0-9][A-Za-z0-9_.-]*\.json$`)

type Relay struct {
	client      *http.Client
	upstreamURL string
	token       string
	exportPath  string
	mu          sync.Mutex // one fetch-write-cleanup at a time
}

// New builds a relay. client must carry a timeout; over Tailscale it is the
// tsnet node's HTTP client.
func New(client *http.Client, upstreamURL, token, exportPath string) *Relay {
	return &Relay{client: client, upstreamURL: upstreamURL, token: token, exportPath: exportPath}
}

// Handler serves /export and an unauthenticated /healthz on his network.
func (rl *Relay) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET "+HealthPath, func(w http.ResponseWriter, _ *http.Request) {
		_, _ = w.Write([]byte("ok\n"))
	})
	mux.HandleFunc("GET "+ExportPath, func(w http.ResponseWriter, r *http.Request) {
		name, content, err := rl.run(r)
		if err != nil {
			logging.Error("relay: export failed, no file written", "error", err)
			http.Error(w, "export failed: "+err.Error(), http.StatusBadGateway)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=%q", name))
		_, _ = w.Write(content)
	})
	return mux
}

func (rl *Relay) run(r *http.Request) (string, []byte, error) {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	name, content, err := rl.fetch(r)
	if err != nil {
		return "", nil, err
	}
	if err := file.WriteBytes(content, rl.exportPath, name); err != nil {
		return "", nil, err
	}
	// Same as the CronJob: a failed cleanup fails the run, the file stays.
	if err := file.Cleanup(rl.exportPath); err != nil {
		return "", nil, err
	}
	logging.Info("relay: export written", "file", name)
	return name, content, nil
}

func (rl *Relay) fetch(r *http.Request) (string, []byte, error) {
	req, err := http.NewRequestWithContext(r.Context(), http.MethodGet, rl.upstreamURL, nil)
	if err != nil {
		return "", nil, fmt.Errorf("bad upstream URL: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+rl.token)
	resp, err := rl.client.Do(req)
	if err != nil {
		return "", nil, fmt.Errorf("upstream unreachable: %w", err)
	}
	defer func() { _ = resp.Body.Close() }()
	if resp.StatusCode != http.StatusOK {
		return "", nil, fmt.Errorf("upstream answered %s", resp.Status)
	}
	_, params, err := mime.ParseMediaType(resp.Header.Get("Content-Disposition"))
	name := params["filename"]
	if err != nil || !safeName.MatchString(name) {
		return "", nil, fmt.Errorf("upstream sent no usable file name (Content-Disposition %q)", resp.Header.Get("Content-Disposition"))
	}
	content, err := io.ReadAll(io.LimitReader(resp.Body, maxBody+1))
	if err != nil {
		return "", nil, fmt.Errorf("reading upstream body: %w", err)
	}
	if len(content) > maxBody || !json.Valid(content) {
		return "", nil, fmt.Errorf("upstream body is not a JSON export (%d bytes)", len(content))
	}
	return name, content, nil
}
