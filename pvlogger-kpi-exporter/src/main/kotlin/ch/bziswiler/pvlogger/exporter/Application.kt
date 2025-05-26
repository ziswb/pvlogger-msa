package ch.bziswiler.pvlogger.exporter

import ch.bziswiler.pvlogger.exporter.configuration.appModule
import ch.bziswiler.pvlogger.exporter.configuration.setupConfig
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.event.Level

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    if (!testing) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
    }
    install(CallLogging) { level = Level.TRACE }
    setupConfig()
    startServices()
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

fun Application.startServices() {
    val composer by inject<MqReceiverComposer>()
    composer.compose()
}
