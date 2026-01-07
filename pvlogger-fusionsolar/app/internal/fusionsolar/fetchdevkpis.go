package fusionsolar

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"pvlogger/internal/config"
	"pvlogger/internal/database"
	"pvlogger/internal/model"
	"strconv"
	"time"

	"github.com/jackc/pgx/v5/pgtype"
)

const devTypeIdStringInverter = 1

func (f FusionSolarAccess) FetchDeviceRealData() error {
	log.Default().Println("Fetching device real data...")
	client, err := model.NewClientWithResponses(config.FsUrl(), addTokenClientOption(f.token))
	if err != nil {
		return err
	}
	deviceRealKpiJSONRequestBody := model.DeviceRealKpiJSONRequestBody{DevIds: config.FsConfig().DevIds, DevTypeId: devTypeIdStringInverter}
	response, err := client.DeviceRealKpiWithResponse(context.TODO(), deviceRealKpiJSONRequestBody)
	if err != nil {
		return err
	}
	log.Default().Println("Device real data fetched successfully.")
	f.DevKpiResponse = response
	return nil
}

func (f FusionSolarAccess) SaveDeviceRealKpiResponse() error {
	log.Default().Println("Saving Device Real Kpi Response to database...")
	responseId, err := f.insertDeviceRealKpiResponseParams()
	if err != nil {
		return err
	}
	err = f.insertDeviceRealKpiResponseBasicParams(responseId)
	if err != nil {
		return err
	}
	err = f.insertDeviceRealKpi(responseId)
	if err != nil {
		return err
	}
	log.Default().Println("Device Real Kpi response saved successfully.")
	return nil
}

func (f FusionSolarAccess) insertDeviceRealKpiResponseParams() (int32, error) {
	log.Default().Println("Saving Device Real Kpi Response Params to database...")
	raw, err := json.RawMessage.MarshalJSON(f.DevKpiResponse.Body)
	if err != nil {
		return 0, err
	}
	failCode := int32(f.DevKpiResponse.JSON200.FailCode)
	message := messageAsPgTextDevKpi(f.DevKpiResponse)
	success := boolOrFalseDevKpi(f.DevKpiResponse)
	timestamp := pgtype.Timestamp{Valid: true, Time: time.Now()}
	i, err := f.insertFsResponseParams(failCode, message, raw, success, timestamp)
	log.Default().Println("Device Real Kpi response saved successfully.")
	return i, err
}

func (f FusionSolarAccess) insertDeviceRealKpiResponseBasicParams(responseId int32) error {
	log.Default().Println("Saving Device Real Kpi Basic Params  to database...")
	if f.DevKpiResponse.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	currentTime := pgtype.Int8{Valid: true, Int64: *f.DevKpiResponse.JSON200.Params.CurrentTime}
	devIds := pgtype.Text{Valid: true, String: *f.DevKpiResponse.JSON200.Params.DevIds}
	err := f.insertFsResponseParamsDevice(currentTime, responseId, devIds)
	log.Default().Println("Device Real Kpi response saved successfully.")
	return err
}

func (f FusionSolarAccess) insertFsResponseParamsDevice(currentTime pgtype.Int8, responseId int32, deviceIds pgtype.Text) error {
	log.Default().Println("Saving response params to database...")
	responseParamsBasic := database.InsertFsResponseParamsDeviceParams{
		CurrentTime: currentTime,
		Response:    responseId,
		DevIds:      deviceIds,
	}
	err := database.New(f.Dao.Database.Conn).InsertFsResponseParamsDevice(f.Dao.Database.Ctx, responseParamsBasic)
	log.Default().Println("Response params saved successfully.")
	return err
}

func (f FusionSolarAccess) insertDeviceRealKpi(responseId int32) error {
	log.Default().Println("Saving device real kpi to database...")
	data := *f.DevKpiResponse.JSON200.Data
	for i := 0; i < len(data); i++ {
		log.Default().Println("Saving device real kpi record '" + strconv.Itoa(i) + "' ...")
		d := data[i]
		record := database.InsertFsDeviceRealKpiParams{
			AI:            float8entry(d.DataItemMap.AI),
			AU:            float8entry(d.DataItemMap.AU),
			AbU:           float8entry(d.DataItemMap.AbU),
			ActivePower:   float8entry(d.DataItemMap.ActivePower),
			BI:            float8entry(d.DataItemMap.BI),
			BU:            float8entry(d.DataItemMap.BU),
			BcU:           float8entry(d.DataItemMap.BcU),
			CI:            float8entry(d.DataItemMap.CI),
			CU:            float8entry(d.DataItemMap.CU),
			CaU:           float8entry(d.DataItemMap.CaU),
			CloseTime:     int8entry(d.DataItemMap.CloseTime),
			DayCap:        float8entry(d.DataItemMap.DayCap),
			DevId:         pgtype.Int8{Valid: true, Int64: d.DevId},
			Efficiency:    float8entry(d.DataItemMap.Efficiency),
			ElecFreq:      float8entry(d.DataItemMap.ElecFreq),
			InverterState: float8entry(d.DataItemMap.InverterState),
			Mppt10Cap:     float8entry(d.DataItemMap.Mppt10Cap),
			Mppt1Cap:      float8entry(d.DataItemMap.Mppt1Cap),
			Mppt2Cap:      float8entry(d.DataItemMap.Mppt2Cap),
			Mppt3Cap:      float8entry(d.DataItemMap.Mppt3Cap),
			Mppt4Cap:      float8entry(d.DataItemMap.Mppt4Cap),
			Mppt5Cap:      float8entry(d.DataItemMap.Mppt5Cap),
			Mppt6Cap:      float8entry(d.DataItemMap.Mppt6Cap),
			Mppt7Cap:      float8entry(d.DataItemMap.Mppt7Cap),
			Mppt8Cap:      float8entry(d.DataItemMap.Mppt8Cap),
			Mppt9Cap:      float8entry(d.DataItemMap.Mppt9Cap),
			MpptPower:     float8entry(d.DataItemMap.MpptPower),
			MpptTotalCap:  float8entry(d.DataItemMap.MpptTotalCap),
			OpenTime:      int8entry(d.DataItemMap.OpenTime),
			PowerFactor:   float8entry(d.DataItemMap.PowerFactor),
			Pv10I:         float8entry(d.DataItemMap.Pv10I),
			Pv10U:         float8entry(d.DataItemMap.Pv10U),
			Pv11I:         float8entry(d.DataItemMap.Pv11I),
			Pv11U:         float8entry(d.DataItemMap.Pv11U),
			Pv12I:         float8entry(d.DataItemMap.Pv12I),
			Pv12U:         float8entry(d.DataItemMap.Pv12U),
			Pv13I:         float8entry(d.DataItemMap.Pv13I),
			Pv13U:         float8entry(d.DataItemMap.Pv13U),
			Pv14I:         float8entry(d.DataItemMap.Pv14I),
			Pv14U:         float8entry(d.DataItemMap.Pv14U),
			Pv15I:         float8entry(d.DataItemMap.Pv15I),
			Pv15U:         float8entry(d.DataItemMap.Pv15U),
			Pv16I:         float8entry(d.DataItemMap.Pv16I),
			Pv16U:         float8entry(d.DataItemMap.Pv16U),
			Pv17I:         float8entry(d.DataItemMap.Pv17I),
			Pv17U:         float8entry(d.DataItemMap.Pv17U),
			Pv18I:         float8entry(d.DataItemMap.Pv18I),
			Pv18U:         float8entry(d.DataItemMap.Pv18U),
			Pv19I:         float8entry(d.DataItemMap.Pv19I),
			Pv19U:         float8entry(d.DataItemMap.Pv19U),
			Pv1I:          float8entry(d.DataItemMap.Pv1I),
			Pv1U:          float8entry(d.DataItemMap.Pv1U),
			Pv20I:         float8entry(d.DataItemMap.Pv20I),
			Pv20U:         float8entry(d.DataItemMap.Pv20U),
			Pv21I:         float8entry(d.DataItemMap.Pv21I),
			Pv21U:         float8entry(d.DataItemMap.Pv21U),
			Pv22I:         float8entry(d.DataItemMap.Pv22I),
			Pv22U:         float8entry(d.DataItemMap.Pv22U),
			Pv23I:         float8entry(d.DataItemMap.Pv23I),
			Pv23U:         float8entry(d.DataItemMap.Pv23U),
			Pv24I:         float8entry(d.DataItemMap.Pv24I),
			Pv24U:         float8entry(d.DataItemMap.Pv24U),
			Pv25I:         float8entry(d.DataItemMap.Pv25I),
			Pv25U:         float8entry(d.DataItemMap.Pv25U),
			Pv26I:         float8entry(d.DataItemMap.Pv26I),
			Pv26U:         float8entry(d.DataItemMap.Pv26U),
			Pv27I:         float8entry(d.DataItemMap.Pv27I),
			Pv27U:         float8entry(d.DataItemMap.Pv27U),
			Pv28I:         float8entry(d.DataItemMap.Pv28I),
			Pv28U:         float8entry(d.DataItemMap.Pv28U),
			Pv2I:          float8entry(d.DataItemMap.Pv2I),
			Pv2U:          float8entry(d.DataItemMap.Pv2U),
			Pv3I:          float8entry(d.DataItemMap.Pv3I),
			Pv3U:          float8entry(d.DataItemMap.Pv3U),
			Pv4I:          float8entry(d.DataItemMap.Pv4I),
			Pv4U:          float8entry(d.DataItemMap.Pv4U),
			Pv5I:          float8entry(d.DataItemMap.Pv5I),
			Pv5U:          float8entry(d.DataItemMap.Pv5U),
			Pv6I:          float8entry(d.DataItemMap.Pv6I),
			Pv6U:          float8entry(d.DataItemMap.Pv6U),
			Pv7I:          float8entry(d.DataItemMap.Pv7I),
			Pv7U:          float8entry(d.DataItemMap.Pv7U),
			Pv8I:          float8entry(d.DataItemMap.Pv8I),
			Pv8U:          float8entry(d.DataItemMap.Pv8U),
			Pv9I:          float8entry(d.DataItemMap.Pv9I),
			Pv9U:          float8entry(d.DataItemMap.Pv9U),
			ReactivePower: float8entry(d.DataItemMap.ReactivePower),
			Response:      responseId,
			RunState:      int4entry(d.DataItemMap.RunState),
			Temperature:   float8entry(d.DataItemMap.Temperature),
			TotalCap:      float8entry(d.DataItemMap.TotalCap),
			Type:          model.DEVICE_REAL_KPI.String(),
		}
		err := database.New(f.Dao.Database.Conn).InsertFsDeviceRealKpi(f.Dao.Database.Ctx, record)
		if err != nil {
			return err
		}
		log.Default().Println("... saved device real kpi record '" + strconv.Itoa(i) + "'")
	}
	log.Default().Println("Device  real kpi saved successfully.")
	return nil
}

func int4entry(value *int) pgtype.Int4 {
	if value != nil {
		return pgtype.Int4{Valid: true, Int32: int32(*value)}
	}
	return pgtype.Int4{Valid: false, Int32: 0}
}

func int8entry(value *int64) pgtype.Int8 {
	if value != nil {
		return pgtype.Int8{Valid: true, Int64: *value}
	}
	return pgtype.Int8{Valid: false, Int64: 0}
}

func float8entry(value *float32) pgtype.Float8 {
	if value != nil {
		return pgtype.Float8{Valid: true, Float64: float64(*value)}
	}
	return pgtype.Float8{Valid: false, Float64: 0}
}
