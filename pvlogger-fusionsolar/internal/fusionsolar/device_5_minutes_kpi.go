package fusionsolar

import (
	"context"
	"errors"
	"pv-logger/logging"
	"pvlogger-fusionsolar/internal/model"
	"pvlogger-fusionsolar/internal/repository"
	sharedmodel "pvlogger-shared-modbus/model"
	"time"

	"github.com/jackc/pgx/v5/pgtype"
)

func (f *FusionSolarAccess) FetchDevFiveMinutesData() (*model.DeviceFiveMinutesKpiResponse, error) {
	logging.Info("fetching device 5 minutes KPI data")
	client, err := f.newClient()
	if err != nil {
		return nil, err
	}
	formerKPIs, err := f.repo.SelectFormer5MinDeviceRealKpi(f.db.Ctx)
	if err != nil {
		return nil, err
	}
	nextCollectionDate := determineNextCollectionDate(formerKPIs)
	if nextCollectionDate == nil {
		logging.Info("no device 5 minutes KPIs to fetch")
		return nil, nil
	}
	fiveMinutesKpiJSONRequestBody := model.DeviceFiveMinutesKpiJSONRequestBody{
		CollectTime: nextCollectionDate.UnixMilli(),
		DevTypeId:   sharedmodel.InverterDeviceType.Int(),
		DevIds:      f.cfg.Fs.DevIds,
	}
	response, err := client.DeviceFiveMinutesKpiWithResponse(context.Background(), fiveMinutesKpiJSONRequestBody)
	if err != nil {
		return nil, err
	}
	if err := requireJSON200("DeviceFiveMinutesKpi", response.JSON200 != nil, response.HTTPResponse, response.Body); err != nil {
		return nil, err
	}
	logging.Info("device five minutes KPIs fetched successfully")
	return response, nil
}

func (f *FusionSolarAccess) SaveDev5MinKPIs(response *model.DeviceFiveMinutesKpiResponse) error {
	logging.Info("saving device five minutes KPI response to database")
	responseId, err := f.insertDeviceFiveMinutesKpiResponseParams(response)
	if err != nil {
		return err
	}
	err = f.insertDeviceFiveMinutesKpiResponseBasicParams(responseId, response)
	if err != nil {
		return err
	}
	err = f.insertDeviceFiveMinutesKpi(responseId, response)
	if err != nil {
		return err
	}
	logging.Info("device real KPI response saved successfully")
	return nil
}

func (f *FusionSolarAccess) insertDeviceFiveMinutesKpiResponseParams(response *model.DeviceFiveMinutesKpiResponse) (int32, error) {
	logging.Info("saving device five minutes KPI response params to database")
	i, err := f.insertResponseFromData(deviceFiveMinutesKpiResponseAdapter{response})
	if err != nil {
		return i, err
	}
	logging.Info("device five minutes KPI response saved successfully")
	return i, nil
}

// TODO: generalize this function by introducing an interface for the response; afterwards search and remove redundant code
func (f *FusionSolarAccess) insertDeviceFiveMinutesKpiResponseBasicParams(responseId int32, response *model.DeviceFiveMinutesKpiResponse) error {
	logging.Info("saving device five minutes KPI basic params to database")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	input := repository.FsResponseParamsDeviceInput{
		ResponseID:  responseId,
		DevIds:      pgtype.Text{Valid: true, String: *response.JSON200.Params.DevIds},
		CurrentTime: pgtype.Int8{Valid: true, Int64: *response.JSON200.Params.CurrentTime},
	}
	err := f.repo.InsertFsResponseParamsDevice(f.db.Ctx, input)
	logging.Info("device five minutes KPI response saved successfully")
	return err
}

func determineNextCollectionDate(records map[time.Time]int) *time.Time {
	walker := time.Date(2022, 10, 25, 0, 0, 0, 0, time.UTC)
	for walker.Before(time.Now()) {
		if records[walker] < 200 {
			return &walker
		}
		walker = walker.Add(time.Hour * 24)
	}
	return nil
}

func (f *FusionSolarAccess) insertDeviceFiveMinutesKpi(responseId int32, response *model.DeviceFiveMinutesKpiResponse) error {
	logging.Info("saving device five minutes KPI to database")
	data := *response.JSON200.Data
	for i := 0; i < len(data); i++ {
		logging.Info("saving device five minutes KPI record", "index", i)
		d := data[i]
		input := repository.DeviceRealKpiInput{
			AI:            repository.ToPgFloat8(d.DataItemMap.AI),
			AU:            repository.ToPgFloat8(d.DataItemMap.AU),
			AbU:           repository.ToPgFloat8(d.DataItemMap.AbU),
			ActivePower:   repository.ToPgFloat8(d.DataItemMap.ActivePower),
			BI:            repository.ToPgFloat8(d.DataItemMap.BI),
			BU:            repository.ToPgFloat8(d.DataItemMap.BU),
			BcU:           repository.ToPgFloat8(d.DataItemMap.BcU),
			CI:            repository.ToPgFloat8(d.DataItemMap.CI),
			CU:            repository.ToPgFloat8(d.DataItemMap.CU),
			CaU:           repository.ToPgFloat8(d.DataItemMap.CaU),
			CloseTime:     repository.ToPgInt8(d.DataItemMap.CloseTime),
			DayCap:        repository.ToPgFloat8(d.DataItemMap.DayCap),
			DevId:         pgtype.Int8{Valid: true, Int64: d.DevId},
			Efficiency:    repository.ToPgFloat8(d.DataItemMap.Efficiency),
			ElecFreq:      repository.ToPgFloat8(d.DataItemMap.ElecFreq),
			InverterState: repository.ToPgFloat8(d.DataItemMap.InverterState),
			Mppt10Cap:     repository.ToPgFloat8(d.DataItemMap.Mppt10Cap),
			Mppt1Cap:      repository.ToPgFloat8(d.DataItemMap.Mppt1Cap),
			Mppt2Cap:      repository.ToPgFloat8(d.DataItemMap.Mppt2Cap),
			Mppt3Cap:      repository.ToPgFloat8(d.DataItemMap.Mppt3Cap),
			Mppt4Cap:      repository.ToPgFloat8(d.DataItemMap.Mppt4Cap),
			Mppt5Cap:      repository.ToPgFloat8(d.DataItemMap.Mppt5Cap),
			Mppt6Cap:      repository.ToPgFloat8(d.DataItemMap.Mppt6Cap),
			Mppt7Cap:      repository.ToPgFloat8(d.DataItemMap.Mppt7Cap),
			Mppt8Cap:      repository.ToPgFloat8(d.DataItemMap.Mppt8Cap),
			Mppt9Cap:      repository.ToPgFloat8(d.DataItemMap.Mppt9Cap),
			MpptPower:     repository.ToPgFloat8(d.DataItemMap.MpptPower),
			MpptTotalCap:  repository.ToPgFloat8(d.DataItemMap.MpptTotalCap),
			OpenTime:      repository.ToPgInt8(d.DataItemMap.OpenTime),
			PowerFactor:   repository.ToPgFloat8(d.DataItemMap.PowerFactor),
			Pv10I:         repository.ToPgFloat8(d.DataItemMap.Pv10I),
			Pv10U:         repository.ToPgFloat8(d.DataItemMap.Pv10U),
			Pv11I:         repository.ToPgFloat8(d.DataItemMap.Pv11I),
			Pv11U:         repository.ToPgFloat8(d.DataItemMap.Pv11U),
			Pv12I:         repository.ToPgFloat8(d.DataItemMap.Pv12I),
			Pv12U:         repository.ToPgFloat8(d.DataItemMap.Pv12U),
			Pv13I:         repository.ToPgFloat8(d.DataItemMap.Pv13I),
			Pv13U:         repository.ToPgFloat8(d.DataItemMap.Pv13U),
			Pv14I:         repository.ToPgFloat8(d.DataItemMap.Pv14I),
			Pv14U:         repository.ToPgFloat8(d.DataItemMap.Pv14U),
			Pv15I:         repository.ToPgFloat8(d.DataItemMap.Pv15I),
			Pv15U:         repository.ToPgFloat8(d.DataItemMap.Pv15U),
			Pv16I:         repository.ToPgFloat8(d.DataItemMap.Pv16I),
			Pv16U:         repository.ToPgFloat8(d.DataItemMap.Pv16U),
			Pv17I:         repository.ToPgFloat8(d.DataItemMap.Pv17I),
			Pv17U:         repository.ToPgFloat8(d.DataItemMap.Pv17U),
			Pv18I:         repository.ToPgFloat8(d.DataItemMap.Pv18I),
			Pv18U:         repository.ToPgFloat8(d.DataItemMap.Pv18U),
			Pv19I:         repository.ToPgFloat8(d.DataItemMap.Pv19I),
			Pv19U:         repository.ToPgFloat8(d.DataItemMap.Pv19U),
			Pv1I:          repository.ToPgFloat8(d.DataItemMap.Pv1I),
			Pv1U:          repository.ToPgFloat8(d.DataItemMap.Pv1U),
			Pv20I:         repository.ToPgFloat8(d.DataItemMap.Pv20I),
			Pv20U:         repository.ToPgFloat8(d.DataItemMap.Pv20U),
			Pv21I:         repository.ToPgFloat8(d.DataItemMap.Pv21I),
			Pv21U:         repository.ToPgFloat8(d.DataItemMap.Pv21U),
			Pv22I:         repository.ToPgFloat8(d.DataItemMap.Pv22I),
			Pv22U:         repository.ToPgFloat8(d.DataItemMap.Pv22U),
			Pv23I:         repository.ToPgFloat8(d.DataItemMap.Pv23I),
			Pv23U:         repository.ToPgFloat8(d.DataItemMap.Pv23U),
			Pv24I:         repository.ToPgFloat8(d.DataItemMap.Pv24I),
			Pv24U:         repository.ToPgFloat8(d.DataItemMap.Pv24U),
			Pv25I:         repository.ToPgFloat8(d.DataItemMap.Pv25I),
			Pv25U:         repository.ToPgFloat8(d.DataItemMap.Pv25U),
			Pv26I:         repository.ToPgFloat8(d.DataItemMap.Pv26I),
			Pv26U:         repository.ToPgFloat8(d.DataItemMap.Pv26U),
			Pv27I:         repository.ToPgFloat8(d.DataItemMap.Pv27I),
			Pv27U:         repository.ToPgFloat8(d.DataItemMap.Pv27U),
			Pv28I:         repository.ToPgFloat8(d.DataItemMap.Pv28I),
			Pv28U:         repository.ToPgFloat8(d.DataItemMap.Pv28U),
			Pv2I:          repository.ToPgFloat8(d.DataItemMap.Pv2I),
			Pv2U:          repository.ToPgFloat8(d.DataItemMap.Pv2U),
			Pv3I:          repository.ToPgFloat8(d.DataItemMap.Pv3I),
			Pv3U:          repository.ToPgFloat8(d.DataItemMap.Pv3U),
			Pv4I:          repository.ToPgFloat8(d.DataItemMap.Pv4I),
			Pv4U:          repository.ToPgFloat8(d.DataItemMap.Pv4U),
			Pv5I:          repository.ToPgFloat8(d.DataItemMap.Pv5I),
			Pv5U:          repository.ToPgFloat8(d.DataItemMap.Pv5U),
			Pv6I:          repository.ToPgFloat8(d.DataItemMap.Pv6I),
			Pv6U:          repository.ToPgFloat8(d.DataItemMap.Pv6U),
			Pv7I:          repository.ToPgFloat8(d.DataItemMap.Pv7I),
			Pv7U:          repository.ToPgFloat8(d.DataItemMap.Pv7U),
			Pv8I:          repository.ToPgFloat8(d.DataItemMap.Pv8I),
			Pv8U:          repository.ToPgFloat8(d.DataItemMap.Pv8U),
			Pv9I:          repository.ToPgFloat8(d.DataItemMap.Pv9I),
			Pv9U:          repository.ToPgFloat8(d.DataItemMap.Pv9U),
			ReactivePower: repository.ToPgFloat8(d.DataItemMap.ReactivePower),
			Response:      responseId,
			RunState:      repository.ToPgInt4(d.DataItemMap.RunState),
			Temperature:   repository.ToPgFloat8(d.DataItemMap.Temperature),
			TotalCap:      repository.ToPgFloat8(d.DataItemMap.TotalCap),
			Type:          model.DeviceRealKpi.String(),
		}
		err := f.repo.InsertDeviceRealKpi(f.db.Ctx, input)
		if err != nil {
			return err
		}
		logging.Info("saved device five minutes KPI record", "index", i)
	}
	logging.Info("device five minutes KPI saved successfully")
	return nil
}
