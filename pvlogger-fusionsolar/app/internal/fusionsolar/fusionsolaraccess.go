package fusionsolar

import (
	"pvlogger/internal/auth"
	"pvlogger/internal/config"
	"pvlogger/internal/database"

	"pvlogger-go-shared/logging"
)

type FusionSolarAccess struct {
	token auth.Token
	Dao   database.Dao
}

func NewFusionSolarAccess(token auth.Token) *FusionSolarAccess {
	db, err := database.NewDatabase(config.DbConfig().Host, config.DbConfig().Port, config.DbConfig().DbName, config.DbConfig().Username, config.DbConfig().Password)
	if err != nil {
		logging.Error("failed to create database connection", "error", err)
	}
	return &FusionSolarAccess{
		token: token,
		Dao: database.Dao{
			Database: *db,
		},
	}
}

func (f *FusionSolarAccess) Close() {
	f.Dao.Database.CloseDb()
}
