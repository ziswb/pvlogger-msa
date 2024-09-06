package ch.bziswiler.pvlogger.heartbeat.di

import ch.bziswiler.pvlogger.heartbeat.config.AppConfig
import ch.bziswiler.pvlogger.heartbeat.database.DatabaseFactory
import ch.bziswiler.pvlogger.heartbeat.database.DatabaseFactoryImpl
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobScheduler
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobSchedulerManager
import ch.bziswiler.pvlogger.heartbeat.jobs.management.PvLoggerJobFactory
import org.koin.dsl.module
import org.quartz.spi.JobFactory

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<DatabaseFactory> { DatabaseFactoryImpl(get<AppConfig>()) }
    single<JobSchedulerManager> { JobSchedulerManager(get<AppConfig>()) }
    single<JobScheduler> { JobScheduler(get<JobSchedulerManager>()) }
    single<JobFactory> { PvLoggerJobFactory(get<AppConfig>()) }
}
