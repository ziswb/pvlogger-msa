package ch.bziswiler.pvlogger.heartbeat.jobs.management

import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusDeviceIdentifiersCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusDeviceListCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusInverterBatchedDynamicDataCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusInverterBatchedStaticDataCollectionJob
import ch.bziswiler.pvlogger.messaging.SimpleMessageSender
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.Scheduler
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle
import kotlin.reflect.jvm.jvmName

class PvLoggerJobFactory(private val messageSender: SimpleMessageSender) : JobFactory {

    private val logger = KotlinLogging.logger {}

    override fun newJob(bundle: TriggerFiredBundle?, scheduler: Scheduler?): Job {
        if (bundle == null) {
            throw NotImplementedError("Job Factory error: bundle is null")
        }
        logger.info { "creating new job '${bundle.jobDetail.jobClass.name}'" }
        return when (bundle.jobDetail.jobClass.name) {
            ModbusInverterBatchedDynamicDataCollectionJob::class.jvmName -> ModbusInverterBatchedDynamicDataCollectionJob(messageSender)
            ModbusInverterBatchedStaticDataCollectionJob::class.jvmName -> ModbusInverterBatchedStaticDataCollectionJob(messageSender)
            ModbusDeviceIdentifiersCollectionJob::class.jvmName -> ModbusDeviceIdentifiersCollectionJob(messageSender)
            ModbusDeviceListCollectionJob::class.jvmName -> ModbusDeviceListCollectionJob(messageSender)
            else -> throw NotImplementedError("Job Factory error: No factory found for '${bundle.jobDetail?.jobClass?.name}'")
        }
    }
}
