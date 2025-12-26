package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.configuration.ModbusTcpConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.request.ReadDeviceListRequest
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.rest.StoreModbusValuesMessageSender
import mu.KotlinLogging
import java.time.LocalDateTime.now

class ModbusDeviceListCollectionJob(
    config: ModbusTcpConfig,
    channel: ModbusTcpChannel,
    idProvider: IdProvider,
    persistenceMessageSender: StoreModbusValuesMessageSender
) : ModbusDeviceIdentifiersCollectionJob(config, channel, idProvider, persistenceMessageSender) {

    private val logger = KotlinLogging.logger {}

    override fun execute() {
        logger.debug("ModbusDeviceListCollectionJob.execute()")
        super.execute()
    }

    override fun deviceIdentifiers(
        jobId: String,
        deviceId: UByte,
    ) = ReadDeviceListRequest(jobId, now(), idProvider.nextRequestId, deviceId)
}
