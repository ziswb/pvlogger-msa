package main

import (
	"context"
	"errors"
	"net/http"
	"os/signal"
	"syscall"
	"time"

	"tailscale.com/tsnet"

	"pv-logger/logging"
	"pvlogger-kpi-exporter/internal/config"
	"pvlogger-kpi-exporter/internal/file"
	"pvlogger-kpi-exporter/internal/kpi"
	"pvlogger-kpi-exporter/internal/relay"
	"pvlogger-kpi-exporter/internal/serve"
)

// One binary, three jobs (KPI_EXPORTER_MODE): see internal/config.
func main() {
	switch mode := config.Mode(); mode {
	case config.ModeExport:
		export()
	case config.ModeServe:
		runServe()
	case config.ModeRelay:
		runRelay()
	default:
		logging.Fatal("unknown KPI_EXPORTER_MODE", "mode", mode)
	}
}

// export is today's CronJob: fetch, write the file, clean up.
func export() {
	cfg := config.Load()

	kpis, err := kpi.FetchKpis(cfg)
	if err != nil {
		logging.Fatal("failed to fetch KPIs", "error", err)
	}
	record := kpi.MapToExportRecord(cfg.KeyPrefix, kpis)
	if err := file.WriteJSON(&record, cfg.ExportPath, kpi.FileName(cfg.FilePrefix, kpis.CreatedAt)); err != nil {
		logging.Fatal("failed to write KPI export file", "error", err)
	}
	if err := file.Cleanup(cfg.ExportPath); err != nil {
		logging.Fatal("failed to clean up export directory", "error", err)
	}
}

func runServe() {
	cfg := config.LoadServe()
	handler := serve.NewHandler(cfg.TokenFile, cfg.FilePrefix, cfg.KeyPrefix, func() (*kpi.Values, error) { return kpi.FetchKpis(cfg) })
	listen(cfg.HTTPPort, handler, nil)
}

func runRelay() {
	cfg := config.LoadRelay()
	client := &http.Client{Timeout: 30 * time.Second}
	var ts *tsnet.Server
	if cfg.TSAuthKey != "" {
		// Its own Tailscale node in the operator's tailnet (ADR-0024): nothing to
		// install on the host. The state dir keeps the node's identity across
		// restarts, so the auth key is only needed on the first start.
		ts = &tsnet.Server{
			Hostname: cfg.TSHostname,
			Dir:      cfg.TSStateDir,
			AuthKey:  cfg.TSAuthKey,
			Logf:     func(string, ...any) {}, // tsnet is chatty; errors still surface via Up
		}
		ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
		if _, err := ts.Up(ctx); err != nil {
			cancel()
			logging.Fatal("relay: joining the tailnet failed", "error", err)
		}
		cancel()
		client = ts.HTTPClient()
		client.Timeout = 30 * time.Second
		logging.Info("relay: joined the tailnet", "hostname", cfg.TSHostname)
	} else {
		logging.Warn("relay: TS_AUTHKEY not set, reaching the upstream over the host network")
	}
	rl := relay.New(client, cfg.UpstreamURL, cfg.UpstreamToken, cfg.ExportPath)
	listen(cfg.HTTPPort, rl.Handler(), func() {
		if ts != nil {
			_ = ts.Close()
		}
	})
}

// listen serves on :port until SIGTERM/SIGINT, then drains for up to 10 s.
func listen(port string, handler http.Handler, onStop func()) {
	srv := &http.Server{
		Addr:              ":" + port,
		Handler:           handler,
		ReadHeaderTimeout: 10 * time.Second,
		WriteTimeout:      60 * time.Second,
	}
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGTERM, syscall.SIGINT)
	defer stop()
	go func() {
		<-ctx.Done()
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		_ = srv.Shutdown(shutdownCtx)
	}()
	logging.Info("listening", "port", port)
	if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		logging.Fatal("HTTP server failed", "error", err)
	}
	if onStop != nil {
		onStop()
	}
}
