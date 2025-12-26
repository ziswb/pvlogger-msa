package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.configuration.ModbusTcpConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.rest.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.rest.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.CONFIG
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.STATIC

class ModbusBatchedStaticDataCollectionJob(
    config: ModbusTcpConfig,
    channel: ModbusTcpChannel,
    idProvider: IdProvider,
    masterDataService: MasterDataService,
    persistenceMessageSender: StoreModbusValuesMessageSender,
) : ModbusBatchedDataCollectionJob(config, channel, idProvider, masterDataService, persistenceMessageSender) {

    override val categories = listOf(STATIC, CONFIG)
}
