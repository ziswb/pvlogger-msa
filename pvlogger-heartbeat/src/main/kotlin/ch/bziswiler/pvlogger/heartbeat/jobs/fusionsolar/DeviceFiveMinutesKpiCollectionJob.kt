package ch.bziswiler.pvlogger.heartbeat.jobs.fusionsolar

import ch.bziswiler.pvlogger.heartbeat.jobs.MessageProducingJob
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobConfiguration
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobConfigurationHolder
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DEVICE_5MIN_KPI_JOB
import ch.bziswiler.pvlogger.shared.job.tags.JobStartMessageTag
import ch.bziswiler.pvlogger.shared.messaging.TagsAsMessageSender
import io.micrometer.core.instrument.Metrics.counter
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import java.time.ZonedDateTime

class DeviceFiveMinutesKpiCollectionJob(messageSender: TagsAsMessageSender) : MessageProducingJob(messageSender) {

    private val logger = KotlinLogging.logger {}
    private val counter = counter("device-five-minutes-data-collection-job-execution")

    companion object : JobConfigurationHolder {
        override val config
            get() =
                JobConfiguration(
                    jobId = "device-five-minutes-data-collection-job",
                    triggerId = "device-five-minutes-data-collection-job-trigger",
                    jobGroup = "Fusion Solar Jobs",
                    jobClass = DeviceFiveMinutesKpiCollectionJob::class.java,
                    startTime = ZonedDateTime.now(),
                    intervalInSeconds = 12 * 60 * 60
                )
    }

    override fun execute(context: JobExecutionContext?) {
        counter.increment()
        logger.debug { "DeviceFiveMinutesKpiCollectionJob.execute()" }
        super.sendTagAsMessage(JobStartMessageTag(DEVICE_5MIN_KPI_JOB))
    }
}
