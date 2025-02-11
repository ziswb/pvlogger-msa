package ch.bziswiler.pvlogger.fusionsolar.webservice

import ch.bziswiler.fusionsolar.model.*
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
