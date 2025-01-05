package ch.bziswiler.pvlogger.heartbeat.jobs.general

import ch.bziswiler.pvlogger.heartbeat.jobs.MessageProducingJob
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobConfiguration
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobConfigurationHolder
import ch.bziswiler.pvlogger.heartbeat.jobs.minutes
import ch.bziswiler.pvlogger.shared.job.tags.ExportKpisMessageTag
import ch.bziswiler.pvlogger.shared.messaging.TagsAsMessageSender
import io.micrometer.core.instrument.Metrics
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import java.time.ZonedDateTime

class KpiExportJobTrigger(messageSender: TagsAsMessageSender) : MessageProducingJob(messageSender) {

    private val logger = KotlinLogging.logger {}
    private val counter = Metrics.counter("kpi-export-job-trigger-execution")

    companion object : JobConfigurationHolder {
        override val config
            get() = JobConfiguration(
                jobId = "kpi-export-job-trigger-job-id",
                triggerId = "kpi-export-job-trigger-trigger-id",
                jobGroup = "Export Job Triggers",
                jobClass = KpiExportJobTrigger::class.java,
                startTime = ZonedDateTime.now(),
                intervalInSeconds = 5.minutes
            )
    }

    override fun execute(context: JobExecutionContext?) {
        counter.increment()
        logger.debug { "KpiExportJobTrigger.execute()" }
        super.sendTagAsMessage(ExportKpisMessageTag())
    }
}
