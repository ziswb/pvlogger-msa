package ch.bziswiler.pvlogger.fusionsolar.webservice.impl

import ch.bziswiler.fusionsolar.model.DevFiveMinutesResponse
import ch.bziswiler.fusionsolar.model.DevKpiDayResponse
import ch.bziswiler.fusionsolar.model.DevListRequest
import ch.bziswiler.fusionsolar.model.DevListResponse
import ch.bziswiler.fusionsolar.model.DevPeriodicDataRequest
import ch.bziswiler.fusionsolar.model.DevRealKpiRequest
import ch.bziswiler.fusionsolar.model.DevRealKpiResponse
import ch.bziswiler.fusionsolar.model.EmptyObject
import ch.bziswiler.fusionsolar.model.KpiStationExtendedRequest
import ch.bziswiler.fusionsolar.model.KpiStationExtendedResponse
import ch.bziswiler.fusionsolar.model.KpiStationHourResponse
import ch.bziswiler.fusionsolar.model.StationListResponse
import ch.bziswiler.fusionsolar.model.StationRealKpiRequest
import ch.bziswiler.fusionsolar.model.StationRealKpiResponse
import ch.bziswiler.pvlogger.business.channel.huawei.modbus.DeviceType.INVERTER
import ch.bziswiler.pvlogger.business.model.huawei.fusionsolar.FusionSolarDataType.DEVICE_5_MIN_KPI
import ch.bziswiler.pvlogger.core.application.AppConfig
import ch.bziswiler.pvlogger.fusionsolar.configuration.AppConfig
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarConnection
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataAccess
import ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema.FsDeviceRealKpi
import ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema.FsDeviceRealKpi.collectTime
import ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema.FsDeviceRealKpi.type
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.Instant.ofEpochMilli
import java.time.LocalDate
import java.time.LocalDate.of
import java.time.LocalTime
import java.time.ZoneId.systemDefault
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

class FusionSolarDataAccessImpl(config: AppConfig) : FusionSolarDataAccess {

    private val logger = KotlinLogging.logger {}
    private val stationListUrl = "${config.fusionSolarConfig.baseUrlOld}/getStationList"
    private val stationRealKpiUrl = "${config.fusionSolarConfig.baseUrlOld}/getStationRealKpi"
    private val kpiStationHourUrl = "${config.fusionSolarConfig.baseUrlOld}/getKpiStationHour"
    private val kpiStationDayUrl = "${config.fusionSolarConfig.baseUrlOld}/getKpiStationDay"
    private val kpiStationMonthUrl = "${config.fusionSolarConfig.baseUrlOld}/getKpiStationMonth"
    private val kpiStationYearUrl = "${config.fusionSolarConfig.baseUrlOld}/getKpiStationYear"
    private val devListUrl = "${config.fusionSolarConfig.baseUrlOld}/getDevList"
    private val devRealKpiUrl = "${config.fusionSolarConfig.baseUrlOld}/getDevRealKpi"
    private val devFiveMinutesUrl = "${config.fusionSolarConfig.baseUrlOld}/getDevFiveMinutes"
    private val stringInverterTypeId = 1
    private val connection = FusionSolarConnection(config)
    private var stationCode = ""
    private var deviceIdsList = listOf<Long>()
    private val mutexStationCodes = Mutex()
    private val mutexDeviceIds = Mutex()
    private val deviceIds get() = deviceIdsList.joinToString(",")

    override suspend fun fetchStationRealTimeData(): StationRealKpiResponse {
        assertStationCodes()
        return connection.executeRequest(Url(stationRealKpiUrl), StationRealKpiRequest(stationCode))
    }

    override suspend fun fetchStationHourlyData(): KpiStationHourResponse {
        assertStationCodes()
        return connection.executeRequest(Url(kpiStationHourUrl), KpiStationExtendedRequest(stationCode, requestTime()))
    }

    override suspend fun fetchStationDailyData(): KpiStationExtendedResponse = fetchExtendedResponse(kpiStationDayUrl)

    override suspend fun fetchStationMonthlyData(): KpiStationExtendedResponse =
        fetchExtendedResponse(kpiStationMonthUrl)

    override suspend fun fetchStationYearlyData(): KpiStationExtendedResponse = fetchExtendedResponse(kpiStationYearUrl)

    override suspend fun fetchDevicesRealTimeData(): DevRealKpiResponse {
        assertDeviceIds()
        return connection.executeRequest(Url(devRealKpiUrl), DevRealKpiRequest(deviceIds, stringInverterTypeId))
    }

    override suspend fun fetchDevicesFiveMinutesData(collectTime: Instant): DevFiveMinutesResponse {
        assertDeviceIds()
        return connection.executeRequest(
            Url(devFiveMinutesUrl),
            DevPeriodicDataRequest(deviceIds, INVERTER.deviceTypeId, collectTime.toEpochMilli())
        )
    }

    override suspend fun fetchDevicesDailyData(): DevKpiDayResponse {
        assertDeviceIds()
        return connection.executeRequest(
            Url(devFiveMinutesUrl),
            DevPeriodicDataRequest(deviceIds, INVERTER.deviceTypeId, requestTime())
        )
    }

    override suspend fun nextCollectTimeForFiveMinutesData(): ZonedDateTime? {
        return transaction {
            logger.debug { "FusionSolarDataAccessImpl.nextCollectTimeForFiveMinutesData()" }
            FsDeviceRealKpi
                .select(collectTime)
                .where { type.eq(DEVICE_5_MIN_KPI) and collectTime.isNotNull() }
                .mapNotNull { convertToDate(it[collectTime]) }
                .groupingBy { it }.eachCount()
                .let { existingCollectionDates -> determineNextCollectionDate(existingCollectionDates) }
        }
    }

    private fun determineNextCollectionDate(records: Map<LocalDate, Int>): ZonedDateTime? {
        var walker = of(2022, 10, 25)
        while (walker.isBefore(LocalDate.now())) {
            logger.debug { "FusionSolarDataAccessImpl: checking date '${walker}'" }
            if (records.getOrDefault(walker, 0) < 200) {
                logger.debug { "FusionSolarDataAccessImpl: need to fetch five minute data for date '${walker}'" }
                return walker.atTime(LocalTime.MAX).atZone(systemDefault())
            }
            walker = walker.plusDays(1)
        }
        return null
    }

    private fun convertToDate(timestamp: Long?): LocalDate? = timestamp?.let { ofEpochMilli(timestamp).atZone(systemDefault()).toLocalDate() }

    private suspend fun fetchExtendedResponse(urlString: String): KpiStationExtendedResponse {
        assertStationCodes()
        return connection.executeRequest(Url(urlString), KpiStationExtendedRequest(stationCode, requestTime()))
    }

    private fun requestTime() = now().toInstant().toEpochMilli()

    private suspend fun assertStationCodes() {
        if (stationCode.isBlank()) {
            mutexStationCodes.withLock {
                if (stationCode.isBlank()) {
                    updateStationCode()
                }
            }
        }
    }

    private suspend fun assertDeviceIds() {
        if (deviceIdsList.isEmpty()) {
            mutexDeviceIds.withLock {
                if (deviceIdsList.isEmpty()) {
                    assertStationCodes()
                    updateDeviceIds()
                }
            }
        }
    }

    private suspend fun updateStationCode() {
        logger.info { "updating station code" }
        stationCode = connection.executeRequest<EmptyObject, StationListResponse>(
            Url(stationListUrl),
            EmptyObject()
        ).data!!.first().stationCode
        logger.info { "station code is '$stationCode'" }
    }

    private suspend fun updateDeviceIds() {
        logger.info { "updating device ids" }
        connection.executeRequest<DevListRequest, DevListResponse>(Url(devListUrl), DevListRequest(stationCode))
            .data!!
            .groupBy { devData -> devData.devTypeId }
            .filter { entry -> entry.key == stringInverterTypeId }
            .flatMap { entry -> entry.value }
            .map { devData -> devData.id }
            .let { deviceIdsList = it }
        logger.info { "device ids are '$deviceIds'" }
    }
}
