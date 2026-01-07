package fusionsolar

import (
	"log"
	"pvlogger/internal/auth"
	"pvlogger/internal/config"
	"pvlogger/internal/database"
	"pvlogger/internal/model"
)

type FusionSolarAccess struct {
	DevKpiResponse      *model.DeviceRealKpiResponse
	Dev5MinKpiResponse  *model.DeviceFiveMinutesKpiResponse
	DevListResponse     *model.DeviceListResponse
	StationKpiResponse  *model.StationRealKpiResponse
	StationListResponse *model.StationListResponse
	token               auth.Token
	Dao                 database.Dao
}

func NewFusionSolarAccess(token auth.Token) *FusionSolarAccess {
	db, err := database.NewDatabase(config.DbConfig().Host, config.DbConfig().Port, config.DbConfig().DbName, config.DbConfig().Username, config.DbConfig().Password)
	if err != nil {
		log.Default().Println(err)
	}
	return &FusionSolarAccess{
		token: token,
		Dao: database.Dao{
			Database: *db,
		},
	}
}
func (f FusionSolarAccess) Close() {
	f.Dao.Database.CloseDb()
}
