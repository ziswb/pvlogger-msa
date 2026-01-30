package fusionsolar

import (
	"context"
	"errors"
	"pvlogger-fusionsolar/internal/model"
	"time"

	"pvlogger-go-shared/logging"
)

func (f *FusionSolarAccess) FetchMonthData() (*model.KpiStationMonthResponse, error) {
	logging.Info("fetching month data")
	client, err := f.newClient()
	if err != nil {
		return nil, err
	}
	kpiStationMonthJSONRequestBody := model.KpiStationMonthJSONRequestBody{StationCodes: f.cfg.Fs.StationCode, CollectTime: time.Now().UnixMilli()}
	response, err := client.KpiStationMonthWithResponse(context.Background(), kpiStationMonthJSONRequestBody)
	if err != nil {
		return nil, err
	}
	logging.Info("station month KPIs fetched successfully")
	return response, nil
}

func (f *FusionSolarAccess) SaveStationMonthKpiResponse(response *model.KpiStationMonthResponse) error {
	logging.Info("saving station month KPI response to database")
	responseId, err := f.insertStationMonthKpiResponseParams(response)
	if err != nil {
		return err
	}
	err = f.insertStationMonthKpiResponseBasicParams(responseId, response) // FsResponseParamsExtended.save
	if err != nil {
		return err
	}
	return f.upsertStationMonthKpi(responseId, response) // saveExtendedData
}

func (f *FusionSolarAccess) insertStationMonthKpiResponseParams(response *model.KpiStationMonthResponse) (int32, error) {
	logging.Info("saving station month KPI response params to database")
	i, err := f.insertResponseFromData(kpiStationMonthResponseAdapter{response})
	logging.Info("station month KPI response saved successfully")
	return i, err
}

func (f *FusionSolarAccess) insertStationMonthKpiResponseBasicParams(responseId int32, response *model.KpiStationMonthResponse) error {
	logging.Info("saving station month KPI basic params to database")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	p := response.JSON200.Params
	err := f.insertStationExtendedParams(responseId, p.StationCodes, p.CurrentTime, p.CollectTime)
	logging.Info("station month KPI response saved successfully")
	return err
}

func (f *FusionSolarAccess) upsertStationMonthKpi(responseId int32, response *model.KpiStationMonthResponse) error {
	logging.Info("saving station month KPI to database")
	data := *response.JSON200.Data
	err := f.upsertExtendedKpis(responseId, model.StationMonthKpi, data)
	if err != nil {
		return err
	}
	return nil
}
