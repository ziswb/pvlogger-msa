package ch.bziswiler.pvlogger.heartbeat.jobs.modbus

import ch.bziswiler.pvlogger.heartbeat.jobs.MessageProducingJob
import ch.bziswiler.pvlogger.heartbeat.jobs.management.ConfigurableJobConfigurationHolder
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobConfiguration
import ch.bziswiler.pvlogger.messaging.meta.JobName.DEVICE_IDS_JOB
import ch.bziswiler.pvlogger.messaging.meta.JobStartMessageTag
import io.micrometer.core.instrument.Metrics.counter
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import java.time.ZonedDateTime

open class ModbusDeviceIdentifiersCollectionJob : MessageProducingJob() {

    private val logger = KotlinLogging.logger {}
    private val counter = counter("modbus-device-identifiers-collection-job-execution")

    companion object : ConfigurableJobConfigurationHolder {
        override fun config(start: ZonedDateTime) = JobConfiguration(
            jobId = "modbus-device-identifiers-collection-job",
            triggerId = "modbus-device-identifiers-collection-job-trigger",
            jobGroup = "Modbus Group",
            jobClass = ModbusDeviceIdentifiersCollectionJob::class.java,
            startTime = start
        )

        override fun config(start: ZonedDateTime, stop: ZonedDateTime) = config(start)
    }

    override fun execute(context: JobExecutionContext?) {
        counter.increment()
        logger.debug { "ModbusInverterBatchedDynamicDataCollectionJob.execute()" }
        super.sendMessage(listOf(JobStartMessageTag(DEVICE_IDS_JOB)))
    }
}
