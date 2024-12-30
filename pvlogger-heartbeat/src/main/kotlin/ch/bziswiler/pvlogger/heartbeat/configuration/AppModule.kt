package ch.bziswiler.pvlogger.heartbeat.configuration

import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobScheduler
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobSchedulerManager
import ch.bziswiler.pvlogger.heartbeat.jobs.management.PvLoggerJobFactory
import ch.bziswiler.pvlogger.shared.databbase.DatabaseFactory
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TagsAsMessageSender
import ch.bziswiler.pvlogger.shared.messaging.TopicName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicSender
import org.koin.dsl.module
import org.quartz.spi.JobFactory

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<TagsAsMessageSender> { ActiveMqTopicSender(get<MqSessionFactory>(), JOB_SCHEDULER, false) }
    single<DatabaseFactory> { DatabaseFactory(get<AppConfig>().databaseConfig) }
    single<JobSchedulerManager> { JobSchedulerManager(get<AppConfig>()) }
    single<JobScheduler> { JobScheduler(get<JobSchedulerManager>()) }
    single<JobFactory> { PvLoggerJobFactory(get<TagsAsMessageSender>()) }
}
