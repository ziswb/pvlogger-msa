package ch.bziswiler.pvlogger.kpi.messaging

import ch.bziswiler.pvlogger.kpi.messaging.data.KpiLiveData
import ch.bziswiler.pvlogger.kpi.messaging.data.KpiTodayData
import ch.bziswiler.pvlogger.kpi.rest.DataAccessorRestProxy
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.JobName
import ch.bziswiler.pvlogger.shared.job.status.JobStatus
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessage
import ch.bziswiler.pvlogger.shared.job.tags.JobFinishedMessageTag
import ch.bziswiler.pvlogger.shared.job.tags.StoreKpisMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
import ch.bziswiler.pvlogger.shared.messaging.TaggedMessageSender
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import javax.jms.Message
import javax.jms.ObjectMessage

class KpiAggregatingMessageHandler(private val adapter: DataAccessorRestProxy, private val messageSender: TaggedMessageSender) : MessageHandler {

    override val messageSelector = JobFinishedMessageTag(JobName.DYNAMIC_DATA_JOB).toSelector()

    private val logger = KotlinLogging.logger {}

    override fun onMessage(message: Message?) = when (message) {
        is ObjectMessage -> handleObjectMessage(message)
        null -> logger.info { "message is null" }
        else -> logger.info { "message is not of type ObjectMessage but: ${message.javaClass::class}" }
    }

    private fun handleObjectMessage(message: ObjectMessage) = when (message.`object`) {
        is JobStatusMessage -> handleJobStatusMessage(message.`object` as JobStatusMessage)
        else -> logger.warn { "message.object is not of type JobStatusMessage: ${message.`object`.javaClass::class}" }
    }

    private fun handleJobStatusMessage(jobStatusMessage: JobStatusMessage) = when (jobStatusMessage.status) {
        JobStatus.SUCCESS -> handleJobStatusSuccessMessage(jobStatusMessage)
        else -> logger.info { "Received job message with state '${jobStatusMessage.status}', which is not 'SUCCESS'" }
    }

    private fun KpiAggregatingMessageHandler.handleJobStatusSuccessMessage(jobStatusMessage: JobStatusMessage) = with(jobStatusMessage) {
        PvLoggerKpiData.from(collectLiveKpis(jobId), collectEnergiesTodaySoFar(jobId), jobId).let { kpis ->
            messageSender.tagAndSendMessage(kpis, StoreKpisMessageTag())
        }
    }

    private fun collectLiveKpis(jobId: JobId) = runBlocking { KpiLiveData(adapter.inverterActivePower(jobId), adapter.meterActivePower(jobId)) }

    private fun collectEnergiesTodaySoFar(jobId: JobId) = runBlocking { KpiTodayData(adapter.inverterEnergyToday(jobId), adapter.meterEnergyToday()) }
}

private fun PvLoggerKpiData.Companion.from(liveKpis: KpiLiveData, todayKpis: KpiTodayData, jobId: JobId) = PvLoggerKpiData(
    jobId.id,
    liveKpis.producing,
    liveKpis.exporting,
    liveKpis.importing,
    liveKpis.consuming,
    liveKpis.selfConsuming,
    todayKpis.produced,
    todayKpis.exported,
    todayKpis.imported,
    todayKpis.consumed,
    todayKpis.selfConsumed
)