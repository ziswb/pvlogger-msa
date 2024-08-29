package ch.bziswiler.pvlogger.heartbeat.jobs.modbus

import ch.bziswiler.pvlogger.heartbeat.jobs.management.ConfigurableJobConfigurationHolder
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobConfiguration
import ch.bziswiler.pvlogger.messaging.meta.JobName.DEVICE_LIST_JOB
import ch.bziswiler.pvlogger.messaging.meta.JobTag
import io.micrometer.core.instrument.Metrics
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import java.time.ZonedDateTime

class ModbusDeviceListCollectionJob : ModbusDeviceIdentifiersCollectionJob() {

    private val logger = KotlinLogging.logger {}
    private val counter = Metrics.counter("modbus-device-list-collection-job-execution")

    companion object : ConfigurableJobConfigurationHolder {
        override fun config(start: ZonedDateTime) = JobConfiguration(
            jobId = "modbus-device-list-collection-job",
            triggerId = "modbus-device-list-collection-job-trigger",
            jobGroup = "Modbus Group",
            jobClass = ModbusDeviceListCollectionJob::class.java,
            startTime = start
        )

        override fun config(start: ZonedDateTime, stop: ZonedDateTime) = config(start)
    }

    override fun execute(context: JobExecutionContext?) {
        counter.increment()
        logger.debug("ModbusDeviceListCollectionJob.execute()")
        super.execute(context)
        super.sendMessage(listOf(JobTag(DEVICE_LIST_JOB)))
    }
}
