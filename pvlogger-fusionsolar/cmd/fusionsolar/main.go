package main

import (
	"context"

	"pvlogger-fusionsolar/internal/auth"
	"pvlogger-fusionsolar/internal/config"
	"pvlogger-fusionsolar/internal/fusionsolar"
	"pvlogger-fusionsolar/internal/jobs"
	"pvlogger-fusionsolar/internal/k8s"

	"pv-logger/logging"
)

func main() {
	cfg := config.Load()
	token, err := getToken(cfg)
	if err != nil {
		logging.Fatal("failed to get token", "error", err)
	}
	logging.Info("starting job", "name", cfg.Job.Name)
	access, err := fusionsolar.NewFusionSolarAccess(cfg, token)
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

func getToken(cfg *config.Config) (auth.Token, error) {
	if cfg.K8s.UseTokenSecret {
		logging.Info("reading token from K8s secret", "useTokenSecret", cfg.K8s.UseTokenSecret)
		secretReader, err := k8s.NewSecretReader(cfg.K8s.Namespace)
		if err != nil {
			return auth.Token{}, err
		}
		tokenValue, err := secretReader.ReadToken(context.Background(), cfg.K8s.TokenSecretName, cfg.K8s.TokenKey)
		if err != nil {
			return auth.Token{}, err
		}
		return auth.Token{Token: tokenValue}, nil
	}

	logging.Info("authenticating directly (USE_TOKEN_SECRET=false)")
	token, err := auth.Authenticate(cfg)
	if err != nil {
		return auth.Token{}, err
	}
	return *token, nil
}
