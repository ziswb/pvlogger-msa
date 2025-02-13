package ch.bziswiler.pvlogger.datacollector.configuration

import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.JobSchedulerMessageSender
import ch.bziswiler.pvlogger.shared.messaging.JobTriggeringMessageHandler
import ch.bziswiler.pvlogger.datacollector.services.messaging.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedDynamicDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedStaticDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceIdentifiersCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceListCollectionJob
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DEVICE_IDS_JOB
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DEVICE_LIST_JOB
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DYNAMIC_DATA_JOB
import ch.bziswiler.pvlogger.shared.job.meta.JobName.STATIC_DATA_JOB
import ch.bziswiler.pvlogger.shared.job.tags.JobStartMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicReceiver
import org.koin.dsl.module
import javax.jms.Topic

val appModule = module {
    single { AppConfig() }
    single { IdProvider() }
    single { ModbusTcpChannel(get<AppConfig>()) }
    single { MasterDataService(get<AppConfig>()) }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<MessageReceiver<Topic>> { ActiveMqTopicReceiver(get<MqSessionFactory>(), JOB_SCHEDULER) }
    single<MqReceiverComposer> {
        ActiveMqReceiverComposer(
            get<MqSessionFactory>(),
            get<MessageReceiver<Topic>>().destination,
            listOf(
                JobTriggeringMessageHandler(
                    JobStartMessageTag(DYNAMIC_DATA_JOB).toSelector(),
                    get<ModbusBatchedDynamicDataCollectionJob>()
                ),
                JobTriggeringMessageHandler(
                    JobStartMessageTag(STATIC_DATA_JOB).toSelector(),
                    get<ModbusBatchedStaticDataCollectionJob>()
                ),
                JobTriggeringMessageHandler(
                    JobStartMessageTag(DEVICE_IDS_JOB).toSelector(),
                    get<ModbusDeviceIdentifiersCollectionJob>()
                ),
                JobTriggeringMessageHandler(
                    JobStartMessageTag(DEVICE_LIST_JOB).toSelector(),
                    get<ModbusDeviceListCollectionJob>()
                )
            )
        )
    }
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
