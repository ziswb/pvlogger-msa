package ch.bziswiler.pvlogger.heartbeat.configuration

import ch.bziswiler.pvlogger.heartbeat.database.DatabaseFactory
import ch.bziswiler.pvlogger.heartbeat.database.DatabaseFactoryImpl
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobScheduler
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobSchedulerManager
import ch.bziswiler.pvlogger.heartbeat.jobs.management.PvLoggerJobFactory
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.TagsMessageSender
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageSender
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import org.koin.dsl.module
import org.quartz.spi.JobFactory

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<TagsMessageSender> { ActiveMqMessageSender(get<MqSessionFactory>(), JOB_SCHEDULER, false) }
    single<DatabaseFactory> { DatabaseFactoryImpl(get<AppConfig>()) }
    single<JobSchedulerManager> { JobSchedulerManager(get<AppConfig>()) }
    single<JobScheduler> { JobScheduler(get<JobSchedulerManager>()) }
    single<JobFactory> { PvLoggerJobFactory(get<TagsMessageSender>()) }
}
