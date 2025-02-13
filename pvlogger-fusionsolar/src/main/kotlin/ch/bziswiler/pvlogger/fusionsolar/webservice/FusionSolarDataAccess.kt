package ch.bziswiler.pvlogger.fusionsolar.webservice

import ch.bziswiler.pvlogger.fusionsolar.model.DevFiveMinutesResponse
import ch.bziswiler.pvlogger.fusionsolar.model.DevKpiDayResponse
import ch.bziswiler.pvlogger.fusionsolar.model.DevRealKpiResponse
import ch.bziswiler.pvlogger.fusionsolar.model.KpiStationExtendedResponse
import ch.bziswiler.pvlogger.fusionsolar.model.KpiStationHourResponse
import ch.bziswiler.pvlogger.fusionsolar.model.StationRealKpiResponse
import java.time.Instant
import java.time.ZonedDateTime

interface FusionSolarDataAccess {

    suspend fun fetchStationRealTimeData(): StationRealKpiResponse
    suspend fun fetchStationHourlyData(): KpiStationHourResponse
    suspend fun fetchStationDailyData(): KpiStationExtendedResponse
    suspend fun fetchStationMonthlyData(): KpiStationExtendedResponse
    suspend fun fetchStationYearlyData(): KpiStationExtendedResponse
    suspend fun fetchDevicesRealTimeData(): DevRealKpiResponse
    suspend fun fetchDevicesFiveMinutesData(collectTime: Instant): DevFiveMinutesResponse
    suspend fun nextCollectTimeForFiveMinutesData(): ZonedDateTime?
    suspend fun fetchDevicesDailyData(): DevKpiDayResponse
}
