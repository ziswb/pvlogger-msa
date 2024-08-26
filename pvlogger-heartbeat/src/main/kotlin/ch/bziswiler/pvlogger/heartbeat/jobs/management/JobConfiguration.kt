package ch.bziswiler.pvlogger.heartbeat.jobs.management

import org.quartz.Job
import org.quartz.JobKey
import org.quartz.TriggerKey
import java.time.Instant
import java.time.ZonedDateTime

data class JobConfiguration(
    val jobId: String,
    val triggerId: String,
    val jobGroup: String,
    val jobClass: Class<out Job>,
    val startTime: ZonedDateTime,
    val stopTime: ZonedDateTime = startTime.plusYears(1000),
    val intervalInSeconds: Int = Int.MAX_VALUE
) {
    val jobKey: JobKey get() = JobKey(jobId, jobGroup)
    val triggerKey: TriggerKey get() = TriggerKey(triggerId, jobGroup)
    val startInstant: Instant get() = startTime.toInstant()
    val stopInstant: Instant get() = stopTime.toInstant()
    val startHour: Int get() = startTime.hour
    val startMinute: Int get() = startTime.minute
}
