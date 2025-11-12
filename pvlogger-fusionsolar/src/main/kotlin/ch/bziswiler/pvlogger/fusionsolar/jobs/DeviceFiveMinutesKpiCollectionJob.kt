package ch.bziswiler.pvlogger.fusionsolar.jobs

import ch.bziswiler.pvlogger.fusionsolar.model.DevFiveMinutesResponse
import ch.bziswiler.pvlogger.fusionsolar.schema.FsDeviceRealKpi
import ch.bziswiler.pvlogger.fusionsolar.schema.FsResponseParamsDevice
import ch.bziswiler.pvlogger.fusionsolar.schema.ResponseObjectMapper.Companion.saveGenericResponse
import ch.bziswiler.pvlogger.fusionsolar.schema.save
import ch.bziswiler.pvlogger.fusionsolar.schema.saveDevFiveMinutesKpi
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataAccess
import ch.bziswiler.pvlogger.shared.job.PvJob
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

class DeviceFiveMinutesKpiCollectionJob(private val fusionSolarDataAccess: FusionSolarDataAccess) : PvJob {

    private val logger = KotlinLogging.logger {}

    override fun execute() {
        doExecute()
    }

    private fun doExecute() = runCatching {
        logger.debug { "DeviceFiveMinutesKpiCollectionJob.execute()" }
        runBlocking { fetchDevFiveMinutesData() }
    }.onSuccess { response: DevFiveMinutesResponse? ->
        persist(response)
    }.onFailure {
        logger.error(it) { "Fetching device five minutes KPI data failed" }
    }

    private suspend fun fetchDevFiveMinutesData(): DevFiveMinutesResponse? {
        val fetchTime = fusionSolarDataAccess.nextCollectTimeForFiveMinutesData()
        return if (fetchTime != null) {
            logger.debug { "DeviceFiveMinutesKpiCollectionJob: collecting five minute data for '${fetchTime}" }
            fusionSolarDataAccess.fetchDevicesFiveMinutesData(fetchTime.toInstant())
        } else {
            logger.debug { "No DeviceFiveMinutesKpiData to fetch" }
            null
        }
    }

    private fun persist(response: DevFiveMinutesResponse?) = response?.let {
        transaction {
            saveGenericResponse(response).let { responseId ->
                FsResponseParamsDevice.save(responseId, response.params)
                FsDeviceRealKpi.saveDevFiveMinutesKpi(responseId, response.data!!)
            }
        }
    }
}
