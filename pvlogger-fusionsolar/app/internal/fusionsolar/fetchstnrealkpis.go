package fusionsolar

import (
	"context"
	"errors"
	"pvlogger/internal/config"
	"pvlogger/internal/database"
	"pvlogger/internal/model"

	"pvlogger-go-shared/logging"

	"github.com/jackc/pgx/v5/pgtype"
)

func (f *FusionSolarAccess) FetchStationRealTimeData() (*model.StationRealKpiResponse, error) {
	logging.Debug("Fetching station real time data...")
	client, err := f.newClient()
	if err != nil {
		return nil, err
	}
	stationRealKpiJSONRequestBody := model.StationRealKpiJSONRequestBody{StationCodes: config.FsConfig().StationCode}
	response, err := client.StationRealKpiWithResponse(context.Background(), stationRealKpiJSONRequestBody)
	if err != nil {
		return nil, err
	}
	logging.Debug("Station real time data fetched successfully.")
	return response, nil
}

func (f *FusionSolarAccess) SaveStationRealKpiResponse(response *model.StationRealKpiResponse) error {
	logging.Debug("Saving response to database...")
	responseId, err := f.insertStationRealKpiResponseParams(response)
	if err != nil {
		return err
	}
	err = f.insertStationRealKpiResponseBasicParams(responseId, response)
	if err != nil {
		return err
	}
	err = f.insertStationRealKpi(responseId, response)
	if err != nil {
		return err
	}
	logging.Debug("Response saved successfully.")
	return nil
}

func (f *FusionSolarAccess) insertStationRealKpiResponseParams(response *model.StationRealKpiResponse) (int32, error) {
	logging.Debug("Saving Station Real Kpi response to database...")
	i, err := f.insertResponseFromData(stationRealKpiResponseAdapter{response})
	logging.Debug("Station Real Kpi response saved successfully.")
	return i, err
}

func (f *FusionSolarAccess) insertStationRealKpiResponseBasicParams(responseId int32, response *model.StationRealKpiResponse) error {
	logging.Debug("Saving Station Real Kpi response params to database...")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	currentTime := pgtype.Int8{Valid: true, Int64: *response.JSON200.Params.CurrentTime}
	stationCodes := pgtype.Text{Valid: true, String: *response.JSON200.Params.StationCodes}
	err := f.insertFsResponseParamsBasicParams(currentTime, responseId, stationCodes)
	logging.Debug("Station Real Kpi response params saved successfully.")
	return err
}

func (f *FusionSolarAccess) insertFsResponseParamsBasicParams(currentTime pgtype.Int8, responseId int32, stationCodes pgtype.Text) error {
	logging.Debug("Saving response params to database...")
	responseParamsBasic := database.InsertFsResponseParamsBasicParams{
		CurrentTime:  currentTime,
		Response:     responseId,
		StationCodes: stationCodes,
	}
	err := database.New(f.Dao.Database.Conn).InsertFsResponseParamsBasic(f.Dao.Database.Ctx, responseParamsBasic)
	logging.Debug("Response params saved successfully.")
	return err
}

func (f *FusionSolarAccess) insertStationRealKpi(responseId int32, response *model.StationRealKpiResponse) error {
	logging.Debug("Saving station real kpi to database...")
	data := *response.JSON200.Data
	for i := 0; i < len(data); i++ {
		record := database.InsertFsStationRealKpiParams{
			DayIncome:       float64(data[i].DataItemMap.DayIncome),
			DayPower:        float64(data[i].DataItemMap.DayPower),
			MonthPower:      float64(data[i].DataItemMap.MonthPower),
			RealHealthState: int32(data[i].DataItemMap.RealHealthState),
			Response:        responseId,
			StationCode:     pgtype.Text{Valid: true, String: data[i].StationCode},
			TotalIncome:     float64(data[i].DataItemMap.TotalIncome),
			TotalPower:      float64(data[i].DataItemMap.TotalPower),
			Type:            model.StationRealKpi.String(),
		}
		err := database.New(f.Dao.Database.Conn).InsertFsStationRealKpi(f.Dao.Database.Ctx, record)
		if err != nil {
			return err
		}
	}
	logging.Debug("Station real kpi saved successfully.")
	return nil
}
