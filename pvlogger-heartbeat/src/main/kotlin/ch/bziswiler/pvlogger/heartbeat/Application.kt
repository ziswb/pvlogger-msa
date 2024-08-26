package ch.bziswiler.pvlogger.heartbeat

import ch.bziswiler.pvlogger.heartbeat.config.AppConfig
import ch.bziswiler.pvlogger.heartbeat.di.appModule
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobScheduler
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobSchedulerManager
import ch.bziswiler.pvlogger.heartbeat.plugins.configureDatabases
import ch.bziswiler.pvlogger.heartbeat.plugins.configureMonitoring
import ch.bziswiler.pvlogger.heartbeat.plugins.contentNegotiation
import ch.bziswiler.pvlogger.heartbeat.plugins.setupConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.quartz.spi.JobFactory

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(testing: Boolean = false) {
    if (!testing) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
    }
    setupConfig()
    configureDatabases()
    setUpJobs()
    configureMonitoring()
    contentNegotiation()
}

private fun Application.setUpJobs() {
    val jobSchedulerManager by inject<JobSchedulerManager>()
    val jobFactory by inject<JobFactory>()
    val jobScheduler by inject<JobScheduler>()
    val appConfig by inject<AppConfig>()
    jobSchedulerManager.startScheduler()
    jobSchedulerManager.scheduler.setJobFactory(jobFactory)
    jobScheduler.initializeJobs(appConfig)
}
