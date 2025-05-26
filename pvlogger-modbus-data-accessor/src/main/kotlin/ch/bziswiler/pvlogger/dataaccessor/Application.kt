package ch.bziswiler.pvlogger.dataaccessor

import ch.bziswiler.pvlogger.dataaccessor.configuration.appModule
import ch.bziswiler.pvlogger.dataaccessor.configuration.configureDatabases
import ch.bziswiler.pvlogger.dataaccessor.configuration.configureMonitoring
import ch.bziswiler.pvlogger.dataaccessor.configuration.configureRouting
import ch.bziswiler.pvlogger.dataaccessor.configuration.configureSerialization
import ch.bziswiler.pvlogger.dataaccessor.configuration.setupConfig
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import mu.KotlinLogging
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject
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
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureRouting()
    startServices(testing)
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

fun Application.startServices(testing: Boolean = false) {
    if (!testing) {
        startStoringModbusValues()
        startStoringKpiValues()
    }
}

private fun Application.startStoringModbusValues() {
    val composer by inject<MqReceiverComposer>(named("modbus_values"))
    composer.compose()
}

private fun Application.startStoringKpiValues() {
    val composer by inject<MqReceiverComposer>(named("kpi_values"))
    composer.compose()
}