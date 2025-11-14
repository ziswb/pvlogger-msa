package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.configuration.ModbusTcpConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.request.ReadDeviceIdentifiersRequest
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.rest.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ReadDeviceIdentifiersResponse
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.LocalDateTime.now

@OptIn(ExperimentalUnsignedTypes::class)
open class ModbusDeviceIdentifiersCollectionJob(
    config: ModbusTcpConfig,
    private val channel: ModbusTcpChannel,
    protected val idProvider: IdProvider,
    private val persistenceMessageSender: StoreModbusValuesMessageSender
) : ModbusDataCollectionJob {

    private val logger = KotlinLogging.logger {}

    private val inverterDeviceIds = config.inverterDeviceIds

    override fun execute() {
        executeForEachDeviceId(idProvider.nextJobId)
    }

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
        persistenceMessageSender.sendValuesToStore(response)
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
