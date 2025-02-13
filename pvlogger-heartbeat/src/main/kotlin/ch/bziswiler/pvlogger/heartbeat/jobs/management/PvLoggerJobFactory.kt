package ch.bziswiler.pvlogger.heartbeat.jobs.management

import ch.bziswiler.pvlogger.heartbeat.jobs.fusionsolar.DeviceFiveMinutesKpiCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.fusionsolar.DeviceKpiCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.fusionsolar.ExtendedKpiCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.fusionsolar.StationKpiCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.general.KpiExportJobTrigger
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusDeviceIdentifiersCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusDeviceListCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusInverterBatchedDynamicDataCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusInverterBatchedStaticDataCollectionJob
import ch.bziswiler.pvlogger.heartbeat.rest.SunriseSunsetRestProxy
import ch.bziswiler.pvlogger.shared.messaging.TagsAsMessageSender
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.Scheduler
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle
import kotlin.reflect.jvm.jvmName

class PvLoggerJobFactory(
    private val jobScheduler: JobScheduler,
    private val sunriseSunsetRestProxy: SunriseSunsetRestProxy,
    private val messageSender: TagsAsMessageSender
) : JobFactory {

    private val logger = KotlinLogging.logger {}

    override fun newJob(bundle: TriggerFiredBundle?, scheduler: Scheduler?): Job {
        if (bundle == null) {
            throw NotImplementedError("Job Factory error: bundle is null")
        }
        logger.info { "creating new job '${bundle.jobDetail.jobClass.name}'" }
        return when (bundle.jobDetail.jobClass.name) {
            DeviceFiveMinutesKpiCollectionJob::class.jvmName -> DeviceFiveMinutesKpiCollectionJob(messageSender)
            DeviceKpiCollectionJob::class.jvmName -> DeviceKpiCollectionJob(messageSender)
            ExtendedKpiCollectionJob::class.jvmName -> ExtendedKpiCollectionJob(messageSender)
            JobSchedulerJob::class.jvmName -> JobSchedulerJob(sunriseSunsetRestProxy, jobScheduler)
            KpiExportJobTrigger::class.jvmName -> KpiExportJobTrigger(messageSender)
            ModbusDeviceIdentifiersCollectionJob::class.jvmName -> ModbusDeviceIdentifiersCollectionJob(messageSender)
            ModbusDeviceListCollectionJob::class.jvmName -> ModbusDeviceListCollectionJob(messageSender)
            ModbusInverterBatchedDynamicDataCollectionJob::class.jvmName -> ModbusInverterBatchedDynamicDataCollectionJob(messageSender)
            ModbusInverterBatchedStaticDataCollectionJob::class.jvmName -> ModbusInverterBatchedStaticDataCollectionJob(messageSender)
            StationKpiCollectionJob::class.jvmName -> StationKpiCollectionJob(messageSender)
            else -> throw NotImplementedError("Job Factory error: No factory found for '${bundle.jobDetail?.jobClass?.name}'")
        }
    }
}
