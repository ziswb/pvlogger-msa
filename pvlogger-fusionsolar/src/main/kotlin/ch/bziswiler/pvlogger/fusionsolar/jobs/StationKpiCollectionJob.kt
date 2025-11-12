package ch.bziswiler.pvlogger.fusionsolar.jobs

import ch.bziswiler.pvlogger.fusionsolar.model.StationRealKpiResponse
import ch.bziswiler.pvlogger.fusionsolar.schema.FsResponseParamsBasic
import ch.bziswiler.pvlogger.fusionsolar.schema.FsStationRealKpi
import ch.bziswiler.pvlogger.fusionsolar.schema.ResponseObjectMapper.Companion.saveGenericResponse
import ch.bziswiler.pvlogger.fusionsolar.schema.save
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataAccess
import ch.bziswiler.pvlogger.shared.job.PvJob
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

class StationKpiCollectionJob(private val dataAccess: FusionSolarDataAccess) : PvJob {

    private val logger = KotlinLogging.logger {}

    override fun execute() {
        doExecute()
    }

    private fun doExecute() = runCatching {
        logger.debug { "StationKpiCollectionJob.execute()" }
        runBlocking { dataAccess.fetchStationRealTimeData() }
    }.onSuccess { response ->
        persist(response)
    }.onFailure {
        logger.error(it) { "Fetching station real time data failed" }
    }

    private fun persist(response: StationRealKpiResponse) = transaction {
        saveGenericResponse(response).let { responseId ->
            FsResponseParamsBasic.save(responseId, response.params)
            FsStationRealKpi.save(responseId, response.data!!.first())
        }
    }
}
