package ch.bziswiler.pvlogger.heartbeat.jobs.modbus

import ch.bziswiler.pvlogger.heartbeat.jobs.MessageProducingJob
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobConfiguration
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobConfigurationHolder
import ch.bziswiler.pvlogger.messaging.meta.JobName.DYNAMIC_DATA_JOB
import ch.bziswiler.pvlogger.messaging.meta.JobStartMessageTag
import ch.bziswiler.pvlogger.messaging.meta.QueueName
import io.micrometer.core.instrument.Metrics.counter
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import java.time.ZonedDateTime.now

class ModbusInverterBatchedDynamicDataCollectionJob : MessageProducingJob() {

    override val queueName = QueueName.JOB_SCHEDULER

    private val logger = KotlinLogging.logger {}
    private val counter = counter("modbus-inverter-batched-dynamic-data-collection-job-execution")

    companion object : JobConfigurationHolder {
        override val config
            get() = JobConfiguration(
                jobId = "modbus-inverter-batched-dynamic-data-collection-job",
                triggerId = "modbus-inverter-batched-dynamic-data-collection-job-trigger",
                jobGroup = "Modbus Group",
                jobClass = ModbusInverterBatchedDynamicDataCollectionJob::class.java,
                startTime = now(),
                intervalInSeconds = 120,
            )
    }

    override fun execute(context: JobExecutionContext?) {
        counter.increment()
        logger.debug { "ModbusInverterBatchedDynamicDataCollectionJob.execute()" }
        super.sendTagsAsMessage(listOf(JobStartMessageTag(DYNAMIC_DATA_JOB)))
    }
}
