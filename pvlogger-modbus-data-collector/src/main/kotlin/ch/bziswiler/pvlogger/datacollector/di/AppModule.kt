package ch.bziswiler.pvlogger.datacollector.di

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.MqService
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.MqServiceImpl
import ch.bziswiler.pvlogger.datacollector.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedDynamicDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedStaticDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceIdentifiersCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceListCollectionJob
import org.koin.dsl.module

val appModule = module {
    single { AppConfig() }
    single { IdProvider() }
    single { ModbusTcpChannel(get<AppConfig>()) }
    single { MasterDataService(get<AppConfig>()) }
    single<MqService> { MqServiceImpl(get<AppConfig>()) }
    single<ModbusBatchedDynamicDataCollectionJob> {
        ModbusBatchedDynamicDataCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<MasterDataService>(),
            get<MqService>()
        )
    }
    single<ModbusBatchedStaticDataCollectionJob> {
        ModbusBatchedStaticDataCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<MasterDataService>(),
            get<MqService>()
        )
    }
    single<ModbusDeviceIdentifiersCollectionJob> {
        ModbusDeviceIdentifiersCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<MqService>()
        )
    }
    single<ModbusDeviceListCollectionJob> {
        ModbusDeviceListCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<MqService>()
        )
    }
}
