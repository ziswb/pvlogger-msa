package ch.bziswiler.pvlogger.kpi.messaging

import ch.bziswiler.pvlogger.shared.job.meta.JobName
import ch.bziswiler.pvlogger.shared.job.status.JobStatus
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessage
import ch.bziswiler.pvlogger.shared.job.tags.JobFinishedMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.rest.PvHttpClient.client
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import javax.jms.Message
import javax.jms.ObjectMessage

class KpiAggregatingMessageHandler : MessageHandler {

    override val messageSelector = JobFinishedMessageTag(JobName.DYNAMIC_DATA_JOB).toSelector()

    private val logger = KotlinLogging.logger {}

    override fun onMessage(message: Message?) {
        logger.debug {
            """
RECEIVED #########################################################################
StoreModbusValuesMessageListener: message received: $message
RECEIVED #########################################################################
"""
        }
        when (message) {
            is ObjectMessage -> handleObjectMessage(message)
            null -> logger.info { "message is null" }
            else -> logger.info { "message is not of type ObjectMessage but: ${message.javaClass::class}" }
        }
    }

    private fun handleObjectMessage(message: ObjectMessage) {
        if (message.`object` is JobStatusMessage) {
            logger.info { "message: ${message.`object` as JobStatusMessage}" }
            handleJobStatusMessage(message.`object` as JobStatusMessage)
        } else {
            logger.info { "message.object is not of type JobStatusMessage: ${message.`object`.javaClass::class}" }
        }
    }

    private fun handleJobStatusMessage(jobStatusMessage: JobStatusMessage) {
        logger.info { "Job Status is '${jobStatusMessage.status}'" }
        logger.info { "Message tag is '${jobStatusMessage.messageTag.value}'" }
        if (jobStatusMessage.status == JobStatus.SUCCESS) {
            val smartMeterActivePowerMasterData = runBlocking { MasterDataCache.masterDataForSmartMeterActivePower() }
            runBlocking {
                val foo = client.get {
                    url {
                        host = "pvlogger-modbus-data-accessor-service"
                        port = 8081
                        pathSegments = listOf("read-latest-register-value", "1", "${smartMeterActivePowerMasterData.registerAddress}")
                        parameters.append("gain", "${smartMeterActivePowerMasterData.gain}")
                    }
                }
                logger.info { foo }
            }
            val inverterActivePowerMasterData = runBlocking { MasterDataCache.masterDataForInverterActivePower() }
            val inverterEnergyYieldMasterData = runBlocking { MasterDataCache.masterDataForInverterEnergyYield() }
            logger.info { smartMeterActivePowerMasterData }
            logger.info { inverterActivePowerMasterData }
            logger.info { inverterEnergyYieldMasterData }
        }
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
