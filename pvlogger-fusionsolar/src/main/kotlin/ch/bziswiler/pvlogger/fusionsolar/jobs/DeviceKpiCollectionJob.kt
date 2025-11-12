package ch.bziswiler.pvlogger.fusionsolar.jobs

import ch.bziswiler.pvlogger.fusionsolar.model.DevRealKpiResponse
import ch.bziswiler.pvlogger.fusionsolar.schema.FsDeviceRealKpi
import ch.bziswiler.pvlogger.fusionsolar.schema.FsResponseParamsDevice
import ch.bziswiler.pvlogger.fusionsolar.schema.ResponseObjectMapper.Companion.saveGenericResponse
import ch.bziswiler.pvlogger.fusionsolar.schema.save
import ch.bziswiler.pvlogger.fusionsolar.schema.saveDevRealKpi
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataAccess
import ch.bziswiler.pvlogger.shared.job.PvJob
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

class DeviceKpiCollectionJob(private val fusionSolarDataAccess: FusionSolarDataAccess) : PvJob {

    private val logger = KotlinLogging.logger {}

    override fun execute() {
        doExecute()
    }

    private fun doExecute() = runCatching {
        logger.debug { "DeviceKpiCollectionJob.execute()" }
        runBlocking { fusionSolarDataAccess.fetchDevicesRealTimeData() }
    }.onSuccess { response ->
        persist(response)
    }.onFailure {
        logger.error(it) { "Fetching device kpi data failed" }
    }

    private fun persist(response: DevRealKpiResponse) = transaction {
        saveGenericResponse(response).let { responseId ->
            FsResponseParamsDevice.save(responseId, response.params)
            FsDeviceRealKpi.saveDevRealKpi(responseId, response.data!!)
        }
    }
}
