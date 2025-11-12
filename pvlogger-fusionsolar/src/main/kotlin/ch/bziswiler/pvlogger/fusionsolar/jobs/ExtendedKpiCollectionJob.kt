package ch.bziswiler.pvlogger.fusionsolar.jobs

import ch.bziswiler.pvlogger.fusionsolar.model.KpiStationExtendedResponse
import ch.bziswiler.pvlogger.fusionsolar.schema.FsResponseParamsExtended
import ch.bziswiler.pvlogger.fusionsolar.schema.ResponseObjectMapper.Companion.saveGenericResponse
import ch.bziswiler.pvlogger.fusionsolar.schema.ResponseToExtendedDataMapper
import ch.bziswiler.pvlogger.fusionsolar.schema.save
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataAccess
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataType
import ch.bziswiler.pvlogger.shared.job.PvJob
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

class ExtendedKpiCollectionJob(private val dataAccess: FusionSolarDataAccess) : PvJob {

    private val logger = KotlinLogging.logger {}

    override fun execute() {
        logger.debug { "ExtendedKpiCollectionJob.execute()" }
        fetchAndSaveDayData()
        fetchAndSaveMonthData()
        fetchAndSaveYearData()
    }

    private fun fetchAndSaveYearData() = runCatching {
        fetchYearData()
    }.onSuccess {
        saveYearData(it)
    }.onFailure {
        logger.error(it) { "fetching and saving year data failed" }
    }

    private fun fetchAndSaveMonthData() = runCatching {
        fetchMonthData()
    }.onSuccess {
        saveMonthData(it)
    }.onFailure {
        logger.error(it) { "fetching and saving month data failed" }
    }

    private fun fetchAndSaveDayData() = runCatching {
        fetchDayData()
    }.onSuccess {
        saveDayData(it)
    }.onFailure {
        logger.error(it) { "fetching and saving day data failed" }
    }

    private fun fetchDayData(): KpiStationExtendedResponse = runBlocking {
        logger.info { "fetching day data" }
        dataAccess.fetchStationDailyData()
    }

    private fun fetchMonthData(): KpiStationExtendedResponse = runBlocking {
        logger.info { "fetching month data" }
        dataAccess.fetchStationMonthlyData()
    }

    private fun fetchYearData(): KpiStationExtendedResponse = runBlocking {
        logger.info { "fetching year data" }
        dataAccess.fetchStationYearlyData()
    }

    private fun saveDayData(data: KpiStationExtendedResponse) = runBlocking {
        logger.info { "saving day data" }
        saveData(data, FusionSolarDataType.STATION_DAY_KPI)
    }

    private fun saveMonthData(data: KpiStationExtendedResponse) = runBlocking {
        logger.info { "saving month data" }
        saveData(data, FusionSolarDataType.STATION_MONTH_KPI)
    }

    private fun saveYearData(data: KpiStationExtendedResponse) = runBlocking {
        logger.info { "saving year data" }
        saveData(data, FusionSolarDataType.STATION_YEAR_KPI)
    }

    private fun saveData(response: KpiStationExtendedResponse, type: FusionSolarDataType) = transaction {
        saveGenericResponse(response).let { responseId ->
            FsResponseParamsExtended.save(responseId, response.params)
            ResponseToExtendedDataMapper.saveExtendedData(responseId, response.data!!, type)
        }
    }
}
