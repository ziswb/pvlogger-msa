package fusionsolar

import (
	"context"
	"errors"
	"log"
	"pvlogger/internal/config"
	"pvlogger/internal/model"
	"time"
)

func (f *FusionSolarAccess) FetchStationDayKpi() (*model.KpiStationDayResponse, error) {
	log.Default().Println("Fetching day data...")
	client, err := f.newClient()
	if err != nil {
		return nil, err
	}
	kpiStationDayJSONRequestBody := model.KpiStationDayJSONRequestBody{StationCodes: config.FsConfig().StationCode, CollectTime: time.Now().UnixMilli()}
	response, err := client.KpiStationDayWithResponse(context.Background(), kpiStationDayJSONRequestBody)
	if err != nil {
		return nil, err
	}
	log.Default().Println("Station Day KPIs fetched successfully.")
	return response, nil
}

func (f *FusionSolarAccess) SaveStationDayKpiResponse(response *model.KpiStationDayResponse) error {
	log.Default().Println("Saving Station Day KPI Response to Database...")
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
	log.Default().Println("Saving Station Day Kpi Response Params to database...")
	i, err := f.insertResponseFromData(kpiStationDayResponseAdapter{response})
	log.Default().Println("Station Day Kpi response saved successfully.")
	return i, err
}

func (f *FusionSolarAccess) insertStationDayKpiResponseBasicParams(responseId int32, response *model.KpiStationDayResponse) error {
	log.Default().Println("Saving Station Day Kpi Basic Params to database...")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	p := response.JSON200.Params
	err := f.insertStationExtendedParams(responseId, p.StationCodes, p.CurrentTime, p.CollectTime)
	log.Default().Println("Station Day Kpi response saved successfully.")
	return err
}
