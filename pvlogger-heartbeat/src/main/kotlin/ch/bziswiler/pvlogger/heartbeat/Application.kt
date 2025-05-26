package ch.bziswiler.pvlogger.heartbeat

import ch.bziswiler.pvlogger.heartbeat.configuration.AppConfig
import ch.bziswiler.pvlogger.heartbeat.configuration.appModule
import ch.bziswiler.pvlogger.heartbeat.configuration.configureDatabases
import ch.bziswiler.pvlogger.heartbeat.configuration.configureMonitoring
import ch.bziswiler.pvlogger.heartbeat.configuration.contentNegotiation
import ch.bziswiler.pvlogger.heartbeat.configuration.setupConfig
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobScheduler
import ch.bziswiler.pvlogger.heartbeat.jobs.management.JobSchedulerManager
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.quartz.spi.JobFactory

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    EngineMain.main(args)
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
    environment.monitor.subscribe(ApplicationStopping) {
        logger.info("Shutting down application...")
    }
    environment.monitor.subscribe(ApplicationStopped) {
        logger.info("Application stopped.")
    }
    environment.monitor.subscribe(ApplicationStopPreparing) {
        logger.info("Preparing to stop application...")
    }
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
