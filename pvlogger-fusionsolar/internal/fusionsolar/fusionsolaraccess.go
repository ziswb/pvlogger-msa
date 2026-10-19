package fusionsolar

import (
	"fmt"
	"pvlogger-fusionsolar/internal/auth"
	"pvlogger-fusionsolar/internal/config"
	"pvlogger-fusionsolar/internal/database"
	"pvlogger-fusionsolar/internal/repository"
)

type FusionSolarAccess struct {
	cfg   *config.Config
	token auth.Token
	db    *database.Database
	repo  repository.Repository
}

func NewFusionSolarAccess(cfg *config.Config, token auth.Token) (*FusionSolarAccess, error) {
	dbCfg := cfg.Database
	db, err := database.NewDatabase(dbCfg.Host, dbCfg.Port, dbCfg.DbName, dbCfg.Username, dbCfg.Password, dbCfg.SslMode)
	if err != nil {
		return nil, fmt.Errorf("failed to create database connection: %w", err)
	}
	return &FusionSolarAccess{
		cfg:   cfg,
		token: token,
		db:    db,
		repo:  repository.NewPostgresRepository(db),
	}, nil
}

func (f *FusionSolarAccess) Close() error {
	if f == nil || f.db == nil {
		return nil
	}
	return f.db.Close()
}
