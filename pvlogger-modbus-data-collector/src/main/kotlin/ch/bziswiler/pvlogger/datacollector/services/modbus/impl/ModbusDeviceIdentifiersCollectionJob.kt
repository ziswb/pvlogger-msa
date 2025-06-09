package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.request.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.modbus.request.ReadDeviceIdentifiersRequest
import ch.bziswiler.pvlogger.datacollector.services.modbus.response.ReadDeviceIdentifiersResponse
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DEVICE_IDS_JOB
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.deviceIdsDataCollectionJobFailedMessage
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.deviceIdsDataCollectionJobSucceededMessage
import ch.bziswiler.pvlogger.shared.job.tags.StoreModbusRegisterValuesMessageTag
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.LocalDateTime.now

@OptIn(ExperimentalUnsignedTypes::class)
open class ModbusDeviceIdentifiersCollectionJob(
    config: AppConfig,
    private val channel: ModbusTcpChannel,
    protected val idProvider: IdProvider,
    private val persistenceMessageSender: StoreModbusValuesMessageSender
) : ModbusDataCollectionJob {

    override val jobName = DEVICE_IDS_JOB

    private val logger = KotlinLogging.logger {}

    private val inverterDeviceIds = config.modBus.inverterDeviceIds

    override fun execute() {
        executeForEachDeviceId(idProvider.nextJobId)
    }

    override fun jobSucceededMessageObject(jobId: JobId) = deviceIdsDataCollectionJobSucceededMessage(jobId)

    override fun jobFailedMessageObject(jobId: JobId) = deviceIdsDataCollectionJobFailedMessage(jobId)

    private fun executeForEachDeviceId(jobId: String) = inverterDeviceIds.forEach { deviceId ->
        executeForSingleDeviceId(jobId, deviceId)
    }

    private fun executeForSingleDeviceId(
        jobId: String,
        deviceId: UByte,
    ) = runCatching {
        logger.debug { "ModbusDeviceIdentifiersCollectionJob.executeForSingleDeviceId().runCatching()" }
        readDeviceIdentifiers(jobId, deviceId)
    }.onSuccess {
        logger.debug { "ModbusDeviceIdentifiersCollectionJob.executeForSingleDeviceId().onSuccess()" }
        persist(it)
    }.onFailure {
        logger.error(it) { "Fetching device identifiers for device '$deviceId' failed" }
        throw it
    }

    private fun persist(response: ReadDeviceIdentifiersResponse) {
        logger.debug { "Storing device identifiers $response" }
        persistenceMessageSender.tagAndSendMessage(response, StoreModbusRegisterValuesMessageTag())
    }

    private fun readDeviceIdentifiers(
        jobId: String,
        deviceId: UByte,
    ): ReadDeviceIdentifiersResponse =
        runBlocking {
            logger.debug { "ModbusDeviceIdentifiersCollectionJob.readAndStoreDeviceIdentifiers()" }
            return@runBlocking channel.request(deviceIdentifiers(jobId, deviceId)) as ReadDeviceIdentifiersResponse
        }

    protected open fun deviceIdentifiers(
        jobId: String,
        deviceId: UByte,
    ) = ReadDeviceIdentifiersRequest(jobId, now(), idProvider.nextRequestId, deviceId)
}
