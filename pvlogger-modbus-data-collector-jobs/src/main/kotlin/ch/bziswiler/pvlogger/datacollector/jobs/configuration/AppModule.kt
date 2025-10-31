package ch.bziswiler.pvlogger.datacollector.jobs.configuration

import ch.bziswiler.pvlogger.datacollector.jobs.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.jobs.services.messaging.JobSchedulerMessageSender
import ch.bziswiler.pvlogger.datacollector.jobs.services.messaging.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.impl.ModbusBatchedDynamicDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.impl.ModbusBatchedStaticDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.impl.ModbusDeviceIdentifiersCollectionJob
import ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.impl.ModbusDeviceListCollectionJob
import ch.bziswiler.pvlogger.modbus.api.meta.JobName
import ch.bziswiler.pvlogger.modbus.api.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.shared.job.tags.JobStartMessageTag
import ch.bziswiler.pvlogger.shared.messaging.JobTriggeringMessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicReceiver
import io.github.config4k.extract
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import javax.jms.Topic

val appModule =
    DI.Module(name = "appModule") {
        bind<IdProvider>() with singleton { IdProvider() }
        bind<ModbusTcpChannel>() with singleton { ModbusTcpChannel(instance()) }
        bind<MasterDataService>() with singleton { MasterDataService(instance()) }
        bind<MqSessionFactory>() with
            singleton {
                ActiveMqSessionFactory(
                    instance<AppConfig>().load().extract<Settings>().mqConfig,
                )
            }
        bind<MessageReceiver<Topic>>() with singleton { ActiveMqTopicReceiver(instance(), JOB_SCHEDULER) }
        bind<MqReceiverComposer>() with
            singleton {
                ActiveMqReceiverComposer(
                    instance(),
                    instance<MessageReceiver<Topic>>().destination,
                    listOf(
                        JobTriggeringMessageHandler(
                            JobStartMessageTag(JobName.DYNAMIC_DATA_JOB).toSelector(),
                            instance<ModbusBatchedDynamicDataCollectionJob>(),
                        ),
                        JobTriggeringMessageHandler(
                            JobStartMessageTag(JobName.STATIC_DATA_JOB).toSelector(),
                            instance<ModbusBatchedStaticDataCollectionJob>(),
                        ),
                        JobTriggeringMessageHandler(
                            JobStartMessageTag(JobName.DEVICE_IDS_JOB).toSelector(),
                            instance<ModbusDeviceIdentifiersCollectionJob>(),
                        ),
                        JobTriggeringMessageHandler(
                            JobStartMessageTag(JobName.DEVICE_LIST_JOB).toSelector(),
                            instance<ModbusDeviceListCollectionJob>(),
                        ),
                    ),
                )
            }
        bind<StoreModbusValuesMessageSender>() with singleton { StoreModbusValuesMessageSender(instance()) }
        bind<JobSchedulerMessageSender>() with singleton { JobSchedulerMessageSender(instance()) }
        bind<MetricsPusher>() with
            singleton {
                MetricsPusher(
                    instance<AppConfig>().load().extract<Settings>().metricsPushConfig,
                )
            }
        bind<ModbusBatchedDynamicDataCollectionJob>() with
            singleton {
                ModbusBatchedDynamicDataCollectionJob(
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                )
            }
        bind<ModbusBatchedStaticDataCollectionJob>() with
            singleton {
                ModbusBatchedStaticDataCollectionJob(
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                )
            }
        bind<ModbusDeviceIdentifiersCollectionJob>() with
            singleton {
                ModbusDeviceIdentifiersCollectionJob(
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                )
            }
        bind<ModbusDeviceListCollectionJob>() with
            singleton {
                ModbusDeviceListCollectionJob(
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                )
            }
    }
