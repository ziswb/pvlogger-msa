package ch.bziswiler.pvlogger.datacollector.configuration

import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.JobSchedulerMessageSender
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.PersistenceMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedDynamicDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedStaticDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceIdentifiersCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceListCollectionJob
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import org.koin.dsl.module

val appModule = module {
    single { AppConfig() }
    single { IdProvider() }
    single { ModbusTcpChannel(get<AppConfig>()) }
    single { MasterDataService(get<AppConfig>()) }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<MessageReceiver> { ActiveMqMessageReceiver(get<MqSessionFactory>(), JOB_SCHEDULER) }
    single<PersistenceMessageSender> { PersistenceMessageSender(get<MqSessionFactory>()) }
    single<JobSchedulerMessageSender> { JobSchedulerMessageSender(get<MqSessionFactory>()) }
    single<ModbusBatchedDynamicDataCollectionJob> {
        ModbusBatchedDynamicDataCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<MasterDataService>(),
            get<PersistenceMessageSender>(),
            get<JobSchedulerMessageSender>()
        )
    }
    single<ModbusBatchedStaticDataCollectionJob> {
        ModbusBatchedStaticDataCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<MasterDataService>(),
            get<PersistenceMessageSender>(),
            get<JobSchedulerMessageSender>()
        )
    }
    single<ModbusDeviceIdentifiersCollectionJob> {
        ModbusDeviceIdentifiersCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<PersistenceMessageSender>()
        )
    }
    single<ModbusDeviceListCollectionJob> {
        ModbusDeviceListCollectionJob(
            get<AppConfig>(),
            get<ModbusTcpChannel>(),
            get<IdProvider>(),
            get<PersistenceMessageSender>()
        )
    }
}
