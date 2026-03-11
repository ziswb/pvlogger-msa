package ch.bziswiler.pvlogger.masterdata.server

import ch.bziswiler.pvlogger.masterdata.server.configuration.appModule
import ch.bziswiler.pvlogger.masterdata.server.configuration.configureDatabases
import ch.bziswiler.pvlogger.masterdata.server.configuration.configureMonitoring
import ch.bziswiler.pvlogger.masterdata.server.configuration.configureRouting
import ch.bziswiler.pvlogger.masterdata.server.configuration.configureSerialization
import ch.bziswiler.pvlogger.masterdata.server.configuration.configureStatusPages
import ch.bziswiler.pvlogger.masterdata.server.configuration.setupConfig
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) = EngineMain.main(args)

@Suppress("unused")
fun Application.module(testing: Boolean = false) {
    if (!testing) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
    }
    setupConfig()
    configureStatusPages()
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureRouting()
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
