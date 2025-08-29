package ch.bziswiler.pvlogger.datacollector

import ch.bziswiler.pvlogger.datacollector.configuration.appModule
import ch.bziswiler.pvlogger.datacollector.configuration.configureMonitoring
import ch.bziswiler.pvlogger.datacollector.configuration.setupConfig
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import mu.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import ch.bziswiler.pvlogger.datacollector.configuration.DataCollectorDI
import org.slf4j.event.Level

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    if (!testing) {
        DataCollectorDI.di = DI {
            import(appModule)
        }
    }
    install(CallLogging) { level = Level.TRACE }
    setupConfig()
    configureMonitoring()
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
    val composer: MqReceiverComposer = DataCollectorDI.di.direct.instance()
    composer.compose()
}
