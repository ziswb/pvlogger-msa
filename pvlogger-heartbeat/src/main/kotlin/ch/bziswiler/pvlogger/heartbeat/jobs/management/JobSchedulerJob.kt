package ch.bziswiler.pvlogger.heartbeat.jobs.management

import ch.bziswiler.pvlogger.heartbeat.jobs.fusionsolar.DeviceKpiCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.fusionsolar.ExtendedKpiCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.fusionsolar.StationKpiCollectionJob
import ch.bziswiler.pvlogger.heartbeat.rest.SunriseSunsetRestProxy
import ch.bziswiler.pvlogger.shared.rest.SunriseSunsetPair
import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.LocalDate.now
import java.time.LocalTime.MIDNIGHT
import java.time.ZoneId.systemDefault
import java.time.ZonedDateTime.of

class JobSchedulerJob(
    private val service: SunriseSunsetRestProxy,
    private val jobScheduler: JobScheduler,
) : Job {

    private val logger = KotlinLogging.logger {}
    private val counter = Metrics.counter("job-scheduler-job-execution")

    companion object : JobConfigurationHolder {
        override val config: JobConfiguration
            get() = JobConfiguration(
                jobId = "job-scheduler-job",
                triggerId = "job-scheduler-job-trigger",
                jobGroup = "Job Management Group",
                jobClass = JobSchedulerJob::class.java,
                startTime = of(now(), MIDNIGHT.plusMinutes(90), systemDefault())
            )
    }

    override fun execute(context: JobExecutionContext?) = runBlocking {
        counter.increment()
        logger.debug { "executing job scheduler job" }
        when (val sunriseSunset = service.retrieveSunriseSunset()) {
            null -> logger.error { "could not retrieve sunrise-sunset" }
            else -> scheduleJobs(sunriseSunset)
        }
    }

    private fun scheduleJobs(sunriseSunset: SunriseSunsetPair) {
        jobScheduler.scheduleLimitedRepeatingJob(DeviceKpiCollectionJob.config(sunriseSunset.sunrise, sunriseSunset.sunset))
        jobScheduler.scheduleLimitedRepeatingJob(StationKpiCollectionJob.config(sunriseSunset.sunrise, sunriseSunset.sunset))
        jobScheduler.scheduleLimitedRepeatingJob(ExtendedKpiCollectionJob.config(sunriseSunset.sunrise, sunriseSunset.sunset))
    }
}
