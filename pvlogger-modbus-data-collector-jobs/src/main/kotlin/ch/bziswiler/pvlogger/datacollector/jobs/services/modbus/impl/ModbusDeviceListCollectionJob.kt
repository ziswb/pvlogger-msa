package ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.jobs.configuration.Settings
import ch.bziswiler.pvlogger.datacollector.jobs.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.jobs.services.messaging.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.modbus.api.meta.JobId
import ch.bziswiler.pvlogger.modbus.api.meta.JobName
import ch.bziswiler.pvlogger.modbus.api.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.modbus.tcp.request.ReadDeviceListRequest
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.deviceListDataCollectionJobFailedMessage
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.deviceListDataCollectionJobSucceededMessage
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import mu.KotlinLogging
import java.time.Duration
import java.time.LocalDateTime.now

class ModbusDeviceListCollectionJob(
    settings: Settings,
    channel: ModbusTcpChannel,
    idProvider: IdProvider,
    persistenceMessageSender: StoreModbusValuesMessageSender,
) : ModbusDeviceIdentifiersCollectionJob(settings, channel, idProvider, persistenceMessageSender) {
    override val jobName = JobName.DEVICE_LIST_JOB

    private val logger = KotlinLogging.logger {}
    private val counter = Metrics.counter("modbus-device-list-collection-job-execution")
    private val timer =
        Timer
            .builder("modbus_device_list_execution_timer")
            .publishPercentiles(0.5, 0.95)
            .publishPercentileHistogram()
            .serviceLevelObjectives(Duration.ofMillis(1500))
            .minimumExpectedValue(Duration.ofMillis(100))
            .maximumExpectedValue(Duration.ofMillis(10000))
            .register(Metrics.globalRegistry)

    override fun execute() {
        counter.increment()
        timer.recordCallable {
            logger.debug("ModbusDeviceListCollectionJob.execute()")
            super.execute()
        }
    }

    override fun jobSucceededMessageObject(jobId: JobId) = deviceListDataCollectionJobSucceededMessage(jobId)

    override fun jobFailedMessageObject(jobId: JobId) = deviceListDataCollectionJobFailedMessage(jobId)

    override fun deviceIdentifiers(
        jobId: String,
        deviceId: UByte,
    ) = ReadDeviceListRequest(jobId, now(), idProvider.nextRequestId, deviceId)
}
