package fusionsolar

import (
	"context"
	"errors"
	"log"
	"pvlogger/internal/config"
	"pvlogger/internal/database"
	"pvlogger/internal/model"
	"strconv"

	"github.com/jackc/pgx/v5/pgtype"
)

const devTypeIdStringInverter = 1

func (f *FusionSolarAccess) FetchDeviceRealData() (*model.DeviceRealKpiResponse, error) {
	log.Default().Println("Fetching device real data...")
	client, err := f.newClient()
	if err != nil {
		return nil, err
	}
	deviceRealKpiJSONRequestBody := model.DeviceRealKpiJSONRequestBody{DevIds: config.FsConfig().DevIds, DevTypeId: devTypeIdStringInverter}
	response, err := client.DeviceRealKpiWithResponse(context.Background(), deviceRealKpiJSONRequestBody)
	if err != nil {
		return nil, err
	}
	log.Default().Println("Device real data fetched successfully.")
	return response, nil
}

func (f *FusionSolarAccess) SaveDeviceRealKpiResponse(response *model.DeviceRealKpiResponse) error {
	log.Default().Println("Saving Device Real Kpi Response to database...")
	responseId, err := f.insertDeviceRealKpiResponseParams(response)
	if err != nil {
		return err
	}
	err = f.insertDeviceRealKpiResponseBasicParams(responseId, response)
	if err != nil {
		return err
	}
	err = f.insertDeviceRealKpi(responseId, response)
	if err != nil {
		return err
	}
	log.Default().Println("Device Real Kpi response saved successfully.")
	return nil
}

func (f *FusionSolarAccess) insertDeviceRealKpiResponseParams(response *model.DeviceRealKpiResponse) (int32, error) {
	log.Default().Println("Saving Device Real Kpi Response Params to database...")
	i, err := f.insertResponseFromData(deviceRealKpiResponseAdapter{response})
	log.Default().Println("Device Real Kpi response saved successfully.")
	return i, err
}

func (f *FusionSolarAccess) insertDeviceRealKpiResponseBasicParams(responseId int32, response *model.DeviceRealKpiResponse) error {
	log.Default().Println("Saving Device Real Kpi Basic Params  to database...")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	currentTime := pgtype.Int8{Valid: true, Int64: *response.JSON200.Params.CurrentTime}
	devIds := pgtype.Text{Valid: true, String: *response.JSON200.Params.DevIds}
	err := f.insertFsResponseParamsDevice(currentTime, responseId, devIds)
	log.Default().Println("Device Real Kpi response saved successfully.")
	return err
}

func (f *FusionSolarAccess) insertFsResponseParamsDevice(currentTime pgtype.Int8, responseId int32, deviceIds pgtype.Text) error {
	log.Default().Println("Saving response params to database...")
	responseParamsBasic := database.InsertFsResponseParamsDeviceParams{
		CurrentTime: currentTime,
		Response:    responseId,
		DevIds:      deviceIds,
	}
	err := database.New(f.Dao.Database.Conn).InsertFsResponseParamsDevice(f.Dao.Database.Ctx, responseParamsBasic)
	if err != nil {
		return err
	}
	log.Default().Println("Response params saved successfully.")
	return nil
}

func (f *FusionSolarAccess) insertDeviceRealKpi(responseId int32, response *model.DeviceRealKpiResponse) error {
	log.Default().Println("Saving device real kpi to database...")
	data := *response.JSON200.Data
	for i := 0; i < len(data); i++ {
		log.Default().Println("Saving device real kpi record '" + strconv.Itoa(i) + "' ...")
		d := data[i]
		record := database.InsertFsDeviceRealKpiParams{
			AI:            toPgFloat8(d.DataItemMap.AI),
			AU:            toPgFloat8(d.DataItemMap.AU),
			AbU:           toPgFloat8(d.DataItemMap.AbU),
			ActivePower:   toPgFloat8(d.DataItemMap.ActivePower),
			BI:            toPgFloat8(d.DataItemMap.BI),
			BU:            toPgFloat8(d.DataItemMap.BU),
			BcU:           toPgFloat8(d.DataItemMap.BcU),
			CI:            toPgFloat8(d.DataItemMap.CI),
			CU:            toPgFloat8(d.DataItemMap.CU),
			CaU:           toPgFloat8(d.DataItemMap.CaU),
			CloseTime:     toPgInt8(d.DataItemMap.CloseTime),
			DayCap:        toPgFloat8(d.DataItemMap.DayCap),
			DevId:         pgtype.Int8{Valid: true, Int64: d.DevId},
			Efficiency:    toPgFloat8(d.DataItemMap.Efficiency),
			ElecFreq:      toPgFloat8(d.DataItemMap.ElecFreq),
			InverterState: toPgFloat8(d.DataItemMap.InverterState),
			Mppt10Cap:     toPgFloat8(d.DataItemMap.Mppt10Cap),
			Mppt1Cap:      toPgFloat8(d.DataItemMap.Mppt1Cap),
			Mppt2Cap:      toPgFloat8(d.DataItemMap.Mppt2Cap),
			Mppt3Cap:      toPgFloat8(d.DataItemMap.Mppt3Cap),
			Mppt4Cap:      toPgFloat8(d.DataItemMap.Mppt4Cap),
			Mppt5Cap:      toPgFloat8(d.DataItemMap.Mppt5Cap),
			Mppt6Cap:      toPgFloat8(d.DataItemMap.Mppt6Cap),
			Mppt7Cap:      toPgFloat8(d.DataItemMap.Mppt7Cap),
			Mppt8Cap:      toPgFloat8(d.DataItemMap.Mppt8Cap),
			Mppt9Cap:      toPgFloat8(d.DataItemMap.Mppt9Cap),
			MpptPower:     toPgFloat8(d.DataItemMap.MpptPower),
			MpptTotalCap:  toPgFloat8(d.DataItemMap.MpptTotalCap),
			OpenTime:      toPgInt8(d.DataItemMap.OpenTime),
			PowerFactor:   toPgFloat8(d.DataItemMap.PowerFactor),
			Pv10I:         toPgFloat8(d.DataItemMap.Pv10I),
			Pv10U:         toPgFloat8(d.DataItemMap.Pv10U),
			Pv11I:         toPgFloat8(d.DataItemMap.Pv11I),
			Pv11U:         toPgFloat8(d.DataItemMap.Pv11U),
			Pv12I:         toPgFloat8(d.DataItemMap.Pv12I),
			Pv12U:         toPgFloat8(d.DataItemMap.Pv12U),
			Pv13I:         toPgFloat8(d.DataItemMap.Pv13I),
			Pv13U:         toPgFloat8(d.DataItemMap.Pv13U),
			Pv14I:         toPgFloat8(d.DataItemMap.Pv14I),
			Pv14U:         toPgFloat8(d.DataItemMap.Pv14U),
			Pv15I:         toPgFloat8(d.DataItemMap.Pv15I),
			Pv15U:         toPgFloat8(d.DataItemMap.Pv15U),
			Pv16I:         toPgFloat8(d.DataItemMap.Pv16I),
			Pv16U:         toPgFloat8(d.DataItemMap.Pv16U),
			Pv17I:         toPgFloat8(d.DataItemMap.Pv17I),
			Pv17U:         toPgFloat8(d.DataItemMap.Pv17U),
			Pv18I:         toPgFloat8(d.DataItemMap.Pv18I),
			Pv18U:         toPgFloat8(d.DataItemMap.Pv18U),
			Pv19I:         toPgFloat8(d.DataItemMap.Pv19I),
			Pv19U:         toPgFloat8(d.DataItemMap.Pv19U),
			Pv1I:          toPgFloat8(d.DataItemMap.Pv1I),
			Pv1U:          toPgFloat8(d.DataItemMap.Pv1U),
			Pv20I:         toPgFloat8(d.DataItemMap.Pv20I),
			Pv20U:         toPgFloat8(d.DataItemMap.Pv20U),
			Pv21I:         toPgFloat8(d.DataItemMap.Pv21I),
			Pv21U:         toPgFloat8(d.DataItemMap.Pv21U),
			Pv22I:         toPgFloat8(d.DataItemMap.Pv22I),
			Pv22U:         toPgFloat8(d.DataItemMap.Pv22U),
			Pv23I:         toPgFloat8(d.DataItemMap.Pv23I),
			Pv23U:         toPgFloat8(d.DataItemMap.Pv23U),
			Pv24I:         toPgFloat8(d.DataItemMap.Pv24I),
			Pv24U:         toPgFloat8(d.DataItemMap.Pv24U),
			Pv25I:         toPgFloat8(d.DataItemMap.Pv25I),
			Pv25U:         toPgFloat8(d.DataItemMap.Pv25U),
			Pv26I:         toPgFloat8(d.DataItemMap.Pv26I),
			Pv26U:         toPgFloat8(d.DataItemMap.Pv26U),
			Pv27I:         toPgFloat8(d.DataItemMap.Pv27I),
			Pv27U:         toPgFloat8(d.DataItemMap.Pv27U),
			Pv28I:         toPgFloat8(d.DataItemMap.Pv28I),
			Pv28U:         toPgFloat8(d.DataItemMap.Pv28U),
			Pv2I:          toPgFloat8(d.DataItemMap.Pv2I),
			Pv2U:          toPgFloat8(d.DataItemMap.Pv2U),
			Pv3I:          toPgFloat8(d.DataItemMap.Pv3I),
			Pv3U:          toPgFloat8(d.DataItemMap.Pv3U),
			Pv4I:          toPgFloat8(d.DataItemMap.Pv4I),
			Pv4U:          toPgFloat8(d.DataItemMap.Pv4U),
			Pv5I:          toPgFloat8(d.DataItemMap.Pv5I),
			Pv5U:          toPgFloat8(d.DataItemMap.Pv5U),
			Pv6I:          toPgFloat8(d.DataItemMap.Pv6I),
			Pv6U:          toPgFloat8(d.DataItemMap.Pv6U),
			Pv7I:          toPgFloat8(d.DataItemMap.Pv7I),
			Pv7U:          toPgFloat8(d.DataItemMap.Pv7U),
			Pv8I:          toPgFloat8(d.DataItemMap.Pv8I),
			Pv8U:          toPgFloat8(d.DataItemMap.Pv8U),
			Pv9I:          toPgFloat8(d.DataItemMap.Pv9I),
			Pv9U:          toPgFloat8(d.DataItemMap.Pv9U),
			ReactivePower: toPgFloat8(d.DataItemMap.ReactivePower),
			Response:      responseId,
			RunState:      toPgInt4(d.DataItemMap.RunState),
			Temperature:   toPgFloat8(d.DataItemMap.Temperature),
			TotalCap:      toPgFloat8(d.DataItemMap.TotalCap),
			Type:          model.DeviceRealKpi.String(),
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
