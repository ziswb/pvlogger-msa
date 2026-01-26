package fusionsolar

import (
	"pvlogger/internal/model"
)

// FsResponseData provides a common interface for extracting response metadata
type FsResponseData interface {
	GetBody() []byte
	GetFailCode() int
	GetMessage() *string
	GetSuccess() *bool
}

// Response adapters for generated types

type kpiStationDayResponseAdapter struct {
	*model.KpiStationDayResponse
}

func (r kpiStationDayResponseAdapter) GetBody() []byte     { return r.Body }
func (r kpiStationDayResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r kpiStationDayResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r kpiStationDayResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }

type kpiStationMonthResponseAdapter struct {
	*model.KpiStationMonthResponse
}

func (r kpiStationMonthResponseAdapter) GetBody() []byte     { return r.Body }
func (r kpiStationMonthResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r kpiStationMonthResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r kpiStationMonthResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }

type kpiStationYearResponseAdapter struct {
	*model.KpiStationYearResponse
}

func (r kpiStationYearResponseAdapter) GetBody() []byte     { return r.Body }
func (r kpiStationYearResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r kpiStationYearResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r kpiStationYearResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }

type stationRealKpiResponseAdapter struct {
	*model.StationRealKpiResponse
}

func (r stationRealKpiResponseAdapter) GetBody() []byte     { return r.Body }
func (r stationRealKpiResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r stationRealKpiResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r stationRealKpiResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }

type deviceRealKpiResponseAdapter struct {
	*model.DeviceRealKpiResponse
}

func (r deviceRealKpiResponseAdapter) GetBody() []byte     { return r.Body }
func (r deviceRealKpiResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r deviceRealKpiResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r deviceRealKpiResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }

type deviceFiveMinutesKpiResponseAdapter struct {
	*model.DeviceFiveMinutesKpiResponse
}

func (r deviceFiveMinutesKpiResponseAdapter) GetBody() []byte     { return r.Body }
func (r deviceFiveMinutesKpiResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r deviceFiveMinutesKpiResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r deviceFiveMinutesKpiResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }
