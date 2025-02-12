package ch.bziswiler.pvlogger.fusionsolar.configuration

import ch.bziswiler.pvlogger.fusionsolar.jobs.DeviceFiveMinutesKpiCollectionJob
import ch.bziswiler.pvlogger.fusionsolar.jobs.DeviceKpiCollectionJob
import ch.bziswiler.pvlogger.fusionsolar.jobs.ExtendedKpiCollectionJob
import ch.bziswiler.pvlogger.fusionsolar.jobs.StationKpiCollectionJob
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataAccess
import ch.bziswiler.pvlogger.fusionsolar.webservice.impl.FusionSolarDataAccessImpl
import ch.bziswiler.pvlogger.shared.databbase.DatabaseFactory
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DEVICE_5MIN_KPI_JOB
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DEVICE_KPI_JOB
import ch.bziswiler.pvlogger.shared.job.meta.JobName.EXTENDED_KPI_JOB
import ch.bziswiler.pvlogger.shared.job.meta.JobName.STATION_KPI_JOB
import ch.bziswiler.pvlogger.shared.job.tags.JobStartMessageTag
import ch.bziswiler.pvlogger.shared.messaging.JobTriggeringMessageHandler
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
    single<AppConfig> { AppConfig() }
    single<DatabaseFactory> { DatabaseFactory(get<AppConfig>().databaseConfig) }
    single<FusionSolarDataAccess> { FusionSolarDataAccessImpl(get<AppConfig>().fusionSolarConfig) }
    single<DeviceFiveMinutesKpiCollectionJob> { DeviceFiveMinutesKpiCollectionJob(get<FusionSolarDataAccess>()) }
    single<DeviceKpiCollectionJob> { DeviceKpiCollectionJob(get<FusionSolarDataAccess>()) }
    single<ExtendedKpiCollectionJob> { ExtendedKpiCollectionJob(get<FusionSolarDataAccess>()) }
    single<StationKpiCollectionJob> { StationKpiCollectionJob(get<FusionSolarDataAccess>()) }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<MessageReceiver<Topic>> { ActiveMqTopicReceiver(get<MqSessionFactory>(), JOB_SCHEDULER) }
    single<MqReceiverComposer> {
        ActiveMqReceiverComposer(
            get<MqSessionFactory>(),
            get<MessageReceiver<Topic>>().destination,
            listOf(
                JobTriggeringMessageHandler(
                    JobStartMessageTag(DEVICE_5MIN_KPI_JOB).toSelector(),
                    get<DeviceFiveMinutesKpiCollectionJob>()
                ),
                JobTriggeringMessageHandler(
                    JobStartMessageTag(DEVICE_KPI_JOB).toSelector(),
                    get<DeviceKpiCollectionJob>()
                ),
                JobTriggeringMessageHandler(
                    JobStartMessageTag(EXTENDED_KPI_JOB).toSelector(),
                    get<ExtendedKpiCollectionJob>()
                ),
                JobTriggeringMessageHandler(
                    JobStartMessageTag(STATION_KPI_JOB).toSelector(),
                    get<StationKpiCollectionJob>()
                )
            )
        )
    }
}
