package fusionsolar

import (
	"context"
	"errors"
	"pvlogger-fusionsolar/internal/model"
	"time"

	"pv-logger/logging"
)

func (f *FusionSolarAccess) FetchYearData() (*model.KpiStationYearResponse, error) {
	logging.Info("fetching year data")
	client, err := f.newClient()
	if err != nil {
		return nil, err
	}
	kpiStationYearJSONRequestBody := model.KpiStationYearJSONRequestBody{StationCodes: f.cfg.Fs.StationCode, CollectTime: time.Now().UnixMilli()}
	response, err := client.KpiStationYearWithResponse(context.Background(), kpiStationYearJSONRequestBody)
	if err != nil {
		return nil, err
	}
	logging.Info("station year KPIs fetched successfully")
	return response, nil
}

func (f *FusionSolarAccess) SaveStationYearKpiResponse(response *model.KpiStationYearResponse) error {
	logging.Info("saving station year KPI response to database")
	responseId, err := f.insertStationYearKpiResponseParams(response) // safeGenericResponse
	if err != nil {
		return err
	}
	err = f.insertStationYearKpiResponseBasicParams(responseId, response) // FsResponseParamsExtended.save
	if err != nil {
		return err
	}
	data := *response.JSON200.Data
	return f.upsertExtendedKpis(responseId, model.StationYearKpi, data) // saveExtendedData
}

func (f *FusionSolarAccess) insertStationYearKpiResponseParams(response *model.KpiStationYearResponse) (int32, error) {
	logging.Info("saving station year KPI response params to database")
	i, err := f.insertResponseFromData(kpiStationYearResponseAdapter{response})
	logging.Info("station year KPI response saved successfully")
	return i, err
}

func (f *FusionSolarAccess) insertStationYearKpiResponseBasicParams(responseId int32, response *model.KpiStationYearResponse) error {
	logging.Info("saving station year KPI basic params to database")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	p := response.JSON200.Params
	err := f.insertStationExtendedParams(responseId, p.StationCodes, p.CurrentTime, p.CollectTime)
	logging.Info("station year KPI response saved successfully")
	return err
}
