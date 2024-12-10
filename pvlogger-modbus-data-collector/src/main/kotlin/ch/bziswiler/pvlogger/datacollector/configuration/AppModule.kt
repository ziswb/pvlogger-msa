package ch.bziswiler.pvlogger.datacollector.configuration

import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.JobSchedulerMessageSender
import ch.bziswiler.pvlogger.datacollector.services.messaging.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedDynamicDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedStaticDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceIdentifiersCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceListCollectionJob
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicReceiver
import org.koin.dsl.module

val appModule = module {
    single { AppConfig() }
    single { IdProvider() }
    single { ModbusTcpChannel(get<AppConfig>()) }
    single { MasterDataService(get<AppConfig>()) }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<MessageReceiver> { ActiveMqTopicReceiver(get<MqSessionFactory>(), JOB_SCHEDULER) }
    single<StoreModbusValuesMessageSender> { StoreModbusValuesMessageSender(get<MqSessionFactory>()) }
    single<JobSchedulerMessageSender> { JobSchedulerMessageSender(get<MqSessionFactory>()) }
    single<ModbusBatchedDynamicDataCollectionJob> {
        ModbusBatchedDynamicDataCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<MasterDataService>(),
            get<StoreModbusValuesMessageSender>(),
            get<JobSchedulerMessageSender>()
        )
    }
    single<ModbusBatchedStaticDataCollectionJob> {
        ModbusBatchedStaticDataCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<MasterDataService>(),
            get<StoreModbusValuesMessageSender>(),
            get<JobSchedulerMessageSender>()
        )
    }
    single<ModbusDeviceIdentifiersCollectionJob> {
        ModbusDeviceIdentifiersCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<StoreModbusValuesMessageSender>()
        )
    }
    single<ModbusDeviceListCollectionJob> {
        ModbusDeviceListCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<StoreModbusValuesMessageSender>()
        )
    }
}
