package ch.bziswiler.pvlogger.fusionsolar.jobs

import ch.bziswiler.pvlogger.fusionsolar.model.StationRealKpiResponse
import ch.bziswiler.pvlogger.fusionsolar.schema.FsResponseParamsBasic
import ch.bziswiler.pvlogger.fusionsolar.schema.FsStationRealKpi
import ch.bziswiler.pvlogger.fusionsolar.schema.ResponseObjectMapper.Companion.saveGenericResponse
import ch.bziswiler.pvlogger.fusionsolar.schema.save
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataAccess
import ch.bziswiler.pvlogger.shared.job.PvJob
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration

class StationKpiCollectionJob(private val dataAccess: FusionSolarDataAccess) : PvJob {

    private val logger = KotlinLogging.logger {}
    private val counter = Metrics.counter("station-data-collection-job-execution")
    private val timer = Timer
        .builder("station_data_collection_job_timer")
        .publishPercentiles(0.5, 0.95)
        .publishPercentileHistogram()
        .serviceLevelObjectives(Duration.ofMillis(1500))
        .minimumExpectedValue(Duration.ofMillis(100))
        .maximumExpectedValue(Duration.ofMillis(10000))
        .register(Metrics.globalRegistry)

    override fun execute() {
        counter.increment()
        timer.recordCallable { doExecute() }
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
