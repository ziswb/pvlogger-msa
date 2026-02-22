package fusionsolar

import (
	"context"
	"errors"
	"pvlogger-fusionsolar/internal/model"
	"pvlogger-fusionsolar/internal/repository"

	"pv-logger/logging"

	"github.com/jackc/pgx/v5/pgtype"
)

func (f *FusionSolarAccess) FetchStationRealTimeData() (*model.StationRealKpiResponse, error) {
	logging.Info("fetching station real time data")
	client, err := f.newClient()
	if err != nil {
		return nil, err
	}
	stationRealKpiJSONRequestBody := model.StationRealKpiJSONRequestBody{StationCodes: f.cfg.Fs.StationCode}
	response, err := client.StationRealKpiWithResponse(context.Background(), stationRealKpiJSONRequestBody)
	if err != nil {
		return nil, err
	}
	logging.Info("station real time data fetched successfully")
	return response, nil
}

func (f *FusionSolarAccess) SaveStationRealKpiResponse(response *model.StationRealKpiResponse) error {
	logging.Info("saving response to database")
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
	logging.Info("response saved successfully")
	return nil
}

func (f *FusionSolarAccess) insertStationRealKpiResponseParams(response *model.StationRealKpiResponse) (int32, error) {
	logging.Info("saving station real KPI response to database")
	i, err := f.insertResponseFromData(stationRealKpiResponseAdapter{response})
	logging.Info("station real KPI response saved successfully")
	return i, err
}

func (f *FusionSolarAccess) insertStationRealKpiResponseBasicParams(responseId int32, response *model.StationRealKpiResponse) error {
	logging.Info("saving station real KPI response params to database")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	input := repository.FsResponseParamsBasicInput{
		ResponseID:   responseId,
		StationCodes: pgtype.Text{Valid: true, String: *response.JSON200.Params.StationCodes},
		CurrentTime:  pgtype.Int8{Valid: true, Int64: *response.JSON200.Params.CurrentTime},
	}
	err := f.repo.InsertFsResponseParamsBasic(f.db.Ctx, input)
	logging.Info("station real KPI response params saved successfully")
	return err
}

func (f *FusionSolarAccess) insertStationRealKpi(responseId int32, response *model.StationRealKpiResponse) error {
	logging.Info("saving station real KPI to database")
	data := *response.JSON200.Data
	for i := 0; i < len(data); i++ {
		input := repository.StationRealKpiInput{
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
		err := f.repo.InsertStationRealKpi(f.db.Ctx, input)
		if err != nil {
			return err
		}
	}
	logging.Info("station real KPI saved successfully")
	return nil
}
