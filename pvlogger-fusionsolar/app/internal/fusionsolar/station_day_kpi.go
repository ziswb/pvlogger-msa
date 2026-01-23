package fusionsolar

import (
	"context"
	"errors"
	"pvlogger/internal/config"
	"pvlogger/internal/model"
	"time"

	"pvlogger-go-shared/logging"
)

func (f *FusionSolarAccess) FetchStationDayKpi() (*model.KpiStationDayResponse, error) {
	logging.Info("fetching day data")
	client, err := f.newClient()
	if err != nil {
		return nil, err
	}
	kpiStationDayJSONRequestBody := model.KpiStationDayJSONRequestBody{StationCodes: config.FsConfig().StationCode, CollectTime: time.Now().UnixMilli()}
	response, err := client.KpiStationDayWithResponse(context.Background(), kpiStationDayJSONRequestBody)
	if err != nil {
		return nil, err
	}
	logging.Info("station day KPIs fetched successfully")
	return response, nil
}

func (f *FusionSolarAccess) SaveStationDayKpiResponse(response *model.KpiStationDayResponse) error {
	logging.Debug("saving station day KPI response to database")
	responseId, err := f.insertStationDayKpiResponseParams(response) // safeGenericResponse
	if err != nil {
		return err
	}
	err = f.insertStationDayKpiResponseBasicParams(responseId, response) // FsResponseParamsExtended.save
	if err != nil {
		return err
	}
	data := *response.JSON200.Data
	return f.upsertExtendedKpis(responseId, model.StationDayKpi, data) // saveExtendedData
}

func (f *FusionSolarAccess) insertStationDayKpiResponseParams(response *model.KpiStationDayResponse) (int32, error) {
	logging.Debug("saving station day KPI response params to database")
	i, err := f.insertResponseFromData(kpiStationDayResponseAdapter{response})
	logging.Debug("station day KPI response saved successfully")
	return i, err
}

func (f *FusionSolarAccess) insertStationDayKpiResponseBasicParams(responseId int32, response *model.KpiStationDayResponse) error {
	logging.Debug("saving station day KPI basic params to database")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	p := response.JSON200.Params
	err := f.insertStationExtendedParams(responseId, p.StationCodes, p.CurrentTime, p.CollectTime)
	logging.Debug("station day KPI response saved successfully")
	return err
}
