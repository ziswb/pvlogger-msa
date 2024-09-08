package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.PersistenceMessageSender
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.JobSchedulerMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusBatchedDataCollectionJob
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.ALARM
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.DYNAMIC
import ch.bziswiler.pvlogger.shared.messaging.job.status.JobStatusMessageFactory.dynamicDataCollectionJobFinishedMessage
import ch.bziswiler.pvlogger.shared.messaging.meta.JobFinishedMessageTag
import ch.bziswiler.pvlogger.shared.messaging.meta.JobId
import ch.bziswiler.pvlogger.shared.messaging.meta.JobName.DYNAMIC_DATA_JOB
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Metrics.counter
import io.micrometer.core.instrument.Timer
import java.time.Duration

class ModbusBatchedDynamicDataCollectionJob(
    config: AppConfig,
    channel: ModbusTcpChannel,
    idProvider: IdProvider,
    masterDataService: MasterDataService,
    persistenceMessageSender: PersistenceMessageSender,
    private val jobSchedulerMessageSender: JobSchedulerMessageSender
) : ModbusBatchedDataCollectionJob(config, channel, idProvider, masterDataService, persistenceMessageSender) {

    private val counterId = "modbus-batched-dynamic-data-collection-job-counter"
    private val timerId = "modbus-batched-dynamic-data-collection-job-timer"

    override val categories = listOf(DYNAMIC, ALARM)
    override val metricCounter: Counter = counter(counterId)
    override val metricTimer: Timer =
        Timer
            .builder(timerId)
            .publishPercentiles(0.5, 0.95)
            .publishPercentileHistogram()
            .serviceLevelObjectives(Duration.ofMillis(1500))
            .minimumExpectedValue(Duration.ofMillis(100))
            .maximumExpectedValue(Duration.ofMillis(10000))
            .register(Metrics.globalRegistry)

    override fun jobFinished(jobId: JobId) {
        jobSchedulerMessageSender.tagAndSendMessage(dynamicDataCollectionJobFinishedMessage(jobId), JobFinishedMessageTag(DYNAMIC_DATA_JOB))
    }

    override fun jobFailed(jobId: JobId) {
        jobSchedulerMessageSender.tagAndSendMessage(dynamicDataCollectionJobFinishedMessage(jobId), JobFinishedMessageTag(DYNAMIC_DATA_JOB))
    }
}

//    private val smartMeterDeviceId = 1
//    private val solarInverterId1 = 1
//    private val solarInverterId2 = 16
//    private val scope = CoroutineScope(Dispatchers.Unconfined)
//    fun doPostExecute(jobId: String): Unit = Unit
// send message so another service computes the KPIs
//        transaction {
//            HousePowerCalculator(listOf(solarInverterId1, solarInverterId2), smartMeterDeviceId)
//                .computeHousePowerKpis()
//                .let { kpiValues ->
//                    reportKpiMetrics(kpiValues)
//                    return@transaction HousePowerKpiTable.storeAndGetId(kpiValues, jobId)
//                }
//        }.let {
//            scope.launch {
//                logger.info { "publishing kpi ..." }
//                eventBus.publishEvent(it.value)
//            }
//        }

// this belongs to the dedicated KPI-computation-service
//    private val importedGauge = Metrics.globalRegistry.gauge("kpi_imported_wh", AtomicInteger(0))
//    private val importGauge = Metrics.globalRegistry.gauge("kpi_import_w", AtomicInteger(0))
//    private val exportedGauge = Metrics.globalRegistry.gauge("kpi_exported_wh", AtomicInteger(0))
//    private val exportGauge = Metrics.globalRegistry.gauge("kpi_export_w", AtomicInteger(0))
//    private val consumedGauge = Metrics.globalRegistry.gauge("kpi_consumed_wh", AtomicInteger(0))
//    private val consumeGauge = Metrics.globalRegistry.gauge("kpi_consume_w", AtomicInteger(0))
//    private fun reportKpiMetrics(kpiValues: HousePowerKpis) {
//        importedGauge?.set(kpiValues.dailyEnergyImported.times(1000).toInt())
//        exportedGauge?.set(kpiValues.dailyEnergyExported.times(1000).toInt())
//        consumedGauge?.set(kpiValues.dailyEnergyConsumed.times(1000).toInt())
//        importGauge?.set(kpiValues.gridToHousePower.times(1000).toInt())
//        exportGauge?.set(kpiValues.solarToGridPower.times(1000).toInt())
//        logger.debug {
//            "consume gauge value is ${consumeGauge?.get()}"
//        }
//        logger.debug {
//            "setting consume gauge to ${kpiValues.ownConsumption.times(1000).toInt()}"
//        }
//        consumeGauge?.set(kpiValues.ownConsumption.times(1000).toInt())
//        logger.debug {
//            "consume gauge value is now ${consumeGauge?.get()}"
//        }
//    }
