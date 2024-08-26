package ch.bziswiler.pvlogger.heartbeat.jobs.modbus

import ch.bziswiler.pvlogger.heartbeat.jobs.MessageProducingJob
import ch.bziswiler.pvlogger.heartbeat.jobs.management.ConfigurableJobConfigurationHolder
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobConfiguration
import ch.bziswiler.pvlogger.messaging.job.naming.JobName.STATIC_DATA_JOB
import ch.bziswiler.pvlogger.messaging.job.tag.JobTag
import io.micrometer.core.instrument.Metrics.counter
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import java.time.ZonedDateTime

class ModbusInverterBatchedStaticDataCollectionJob : MessageProducingJob() {

    private val logger = KotlinLogging.logger {}
    private val counter = counter("modbus-inverter-batched-static-data-collection-job-execution")

    companion object : ConfigurableJobConfigurationHolder {
        override fun config(start: ZonedDateTime) = JobConfiguration(
            jobId = "modbus-inverter-batched-static-data-collection-job",
            triggerId = "modbus-inverter-batched-static-data-collection-job-trigger",
            jobGroup = "Modbus Group",
            jobClass = ModbusInverterBatchedStaticDataCollectionJob::class.java,
            startTime = start
        )

        override fun config(start: ZonedDateTime, stop: ZonedDateTime) = config(start)
    }

    override fun execute(context: JobExecutionContext?) {
        counter.increment()
        logger.debug("ModbusInverterBatchedStaticDataCollectionJob.execute()")
        super.sendMessage(listOf(JobTag(STATIC_DATA_JOB)))
    }
}
