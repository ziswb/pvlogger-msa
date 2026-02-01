package main

import (
	"context"

	"pvlogger-fusionsolar-auth/internal/auth"
	"pvlogger-fusionsolar-auth/internal/config"
	"pvlogger-fusionsolar-auth/internal/k8s"

	"pvlogger-go-shared/logging"
)

func main() {
	cfg := config.Load()

	logging.Info("starting fusionsolar-auth job",
		"namespace", cfg.K8s.Namespace,
		"secretName", cfg.K8s.SecretName,
	)

	secretManager, err := k8s.NewSecretManager(cfg.K8s.Namespace)
	if err != nil {
		logging.Fatal("failed to create secret manager", "error", err)
	}

	ctx := context.Background()

	logging.Info("authenticating with FusionSolar API")
	token, err := auth.Authenticate(cfg)
	if err != nil {
		logging.Fatal("authentication failed", "error", err)
	}

	if err := secretManager.UpdateToken(ctx, cfg.K8s.SecretName, cfg.K8s.TokenKey, token); err != nil {
		logging.Fatal("failed to update token secret", "error", err)
	}

	logging.Info("token refreshed and stored successfully")
}
