package ch.bziswiler.pvlogger.kpi.messaging

import ch.bziswiler.pvlogger.kpi.rest.EnergiesRetriever
import ch.bziswiler.pvlogger.shared.job.meta.JobName
import ch.bziswiler.pvlogger.shared.job.status.JobStatus
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessage
import ch.bziswiler.pvlogger.shared.job.tags.JobFinishedMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import javax.jms.Message
import javax.jms.ObjectMessage

class KpiAggregatingMessageHandler(private val adapter: EnergiesRetriever) : MessageHandler {

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
            val kpis = collectLiveKpis()
            logger.debug { "Kpis are: '$kpis'" }
            val energies = collectEnergiesTodaySoFar()
            logger.debug { "Energies are: '$energies'" }
        }
    }

    private fun collectLiveKpis() = runBlocking {
        KpiLiveData(adapter.inverterActivePower(), adapter.meterActivePower())
    }

    private fun collectEnergiesTodaySoFar() = runBlocking {
        EnergiesData(adapter.inverterTodayEnergy(), adapter.meterTodayEnergy())
    }
}
