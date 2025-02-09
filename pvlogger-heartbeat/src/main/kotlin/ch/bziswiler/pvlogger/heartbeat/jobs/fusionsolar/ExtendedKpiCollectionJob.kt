package ch.bziswiler.pvlogger.heartbeat.jobs.fusionsolar

import ch.bziswiler.pvlogger.heartbeat.jobs.MessageProducingJob
import ch.bziswiler.pvlogger.heartbeat.jobs.management.ConfigurableJobConfigurationHolder
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobConfiguration
import ch.bziswiler.pvlogger.shared.job.meta.JobName.EXTENDED_KPI_JOB
import ch.bziswiler.pvlogger.shared.job.tags.JobStartMessageTag
import ch.bziswiler.pvlogger.shared.messaging.TagsAsMessageSender
import io.micrometer.core.instrument.Metrics
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import java.time.ZonedDateTime

class ExtendedKpiCollectionJob(messageSender: TagsAsMessageSender) : MessageProducingJob(messageSender) {

    private val logger = KotlinLogging.logger {}
    private val counter = Metrics.counter("extended-data-collection-job-execution")

    companion object : ConfigurableJobConfigurationHolder {
        override fun config(start: ZonedDateTime) = config(start, start.plusHours(18))

        override fun config(start: ZonedDateTime, stop: ZonedDateTime) = JobConfiguration(
            jobId = "extended-data-collection-job",
            triggerId = "extended-data-collection-job-trigger",
            jobGroup = "Fusion Solar Jobs",
            jobClass = ExtendedKpiCollectionJob::class.java,
            startTime = start,
            stopTime = stop,
            intervalInSeconds = 5 * 60
        )
    }

    override fun execute(context: JobExecutionContext?) {
        counter.increment()
        logger.debug { "ExtendedKpiCollectionJob.execute()" }
        super.sendTagAsMessage(JobStartMessageTag(EXTENDED_KPI_JOB))
    }
}
