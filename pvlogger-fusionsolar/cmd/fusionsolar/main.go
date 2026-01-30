package main

import (
	"pvlogger-fusionsolar/internal/auth"
	"pvlogger-fusionsolar/internal/config"
	"pvlogger-fusionsolar/internal/fusionsolar"
	"pvlogger-fusionsolar/internal/jobs"

	"pvlogger-go-shared/logging"
)

func main() {
	cfg := config.Load()
	token, err := auth.Authenticate(cfg)
	if err != nil {
		logging.Fatal("authentication failed", "error", err)
	}
	logging.Info("starting job", "name", cfg.Job.Name)
	access, err := fusionsolar.NewFusionSolarAccess(cfg, *token)
	if err != nil {
		logging.Fatal("failed to create fusionsolar access", "error", err)
	}
	defer func() {
		if err := access.Close(); err != nil {
			logging.Error("failed to close fusionsolar access", "error", err)
		}
	}()
	jobs.Run(cfg.Job.Name, access)
}
