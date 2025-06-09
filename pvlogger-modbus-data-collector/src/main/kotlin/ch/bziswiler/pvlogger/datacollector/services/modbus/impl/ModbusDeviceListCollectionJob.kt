package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.request.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.modbus.request.ReadDeviceListRequest
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DEVICE_LIST_JOB
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.deviceListDataCollectionJobFailedMessage
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.deviceListDataCollectionJobSucceededMessage
import mu.KotlinLogging
import java.time.LocalDateTime.now

class ModbusDeviceListCollectionJob(
    config: AppConfig,
    channel: ModbusTcpChannel,
    idProvider: IdProvider,
    persistenceMessageSender: StoreModbusValuesMessageSender
) : ModbusDeviceIdentifiersCollectionJob(config, channel, idProvider, persistenceMessageSender) {

    override val jobName = DEVICE_LIST_JOB

    private val logger = KotlinLogging.logger {}

    override fun execute() {
        logger.debug("ModbusDeviceListCollectionJob.execute()")
        super.execute()
    }

    override fun jobSucceededMessageObject(jobId: JobId) = deviceListDataCollectionJobSucceededMessage(jobId)

    override fun jobFailedMessageObject(jobId: JobId) = deviceListDataCollectionJobFailedMessage(jobId)

    override fun deviceIdentifiers(
        jobId: String,
        deviceId: UByte,
    ) = ReadDeviceListRequest(jobId, now(), idProvider.nextRequestId, deviceId)
}
