package fusionsolar

import (
	"fmt"
	"pvlogger/internal/auth"
	"pvlogger/internal/config"
	"pvlogger/internal/database"
	"pvlogger/internal/repository"
)

type FusionSolarAccess struct {
	token auth.Token
	db    *database.Database
	repo  repository.Repository
}

func NewFusionSolarAccess(token auth.Token) (*FusionSolarAccess, error) {
	db, err := database.NewDatabase(config.DbConfig().Host, config.DbConfig().Port, config.DbConfig().DbName, config.DbConfig().Username, config.DbConfig().Password)
	if err != nil {
		return nil, fmt.Errorf("failed to create database connection: %w", err)
	}
	return &FusionSolarAccess{
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
