package ch.bziswiler.pvlogger.datacollector

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedDynamicDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedStaticDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceIdentifiersCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceListCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.rest.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.rest.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.shared.job.meta.JobName
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    startServices()
}

private fun startServices() {
    val job: JobName = JobName.valueOf(AppConfig.jobConfig.name)
    val tcpConfig = AppConfig.modbusTcpConfig
    val mdConfig = AppConfig.masterDataConfig
    println()
    val channel = ModbusTcpChannel(tcpConfig)
    val persistenceMessageSender = StoreModbusValuesMessageSender(AppConfig.masterDataConfig)
    when (job) {
        JobName.DYNAMIC_DATA_JOB ->
            ModbusBatchedDynamicDataCollectionJob(
                tcpConfig,
                channel,
                IdProvider(),
                MasterDataService(mdConfig),
                persistenceMessageSender,
            ).execute()

        JobName.DEVICE_IDS_JOB ->
            ModbusDeviceIdentifiersCollectionJob(
                tcpConfig,
                channel,
                IdProvider(),
                persistenceMessageSender,
            ).execute()

        JobName.DEVICE_LIST_JOB ->
            ModbusDeviceListCollectionJob(
                tcpConfig,
                channel,
                IdProvider(),
                persistenceMessageSender,
            ).execute()

        JobName.STATIC_DATA_JOB ->
            ModbusBatchedStaticDataCollectionJob(
                tcpConfig,
                channel,
                IdProvider(),
                MasterDataService(mdConfig),
                persistenceMessageSender,
            ).execute()

        else -> logger.warn { "Unsupported job name: '$job'" }
    }
}
