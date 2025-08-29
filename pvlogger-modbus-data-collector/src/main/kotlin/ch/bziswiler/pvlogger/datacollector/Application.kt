package ch.bziswiler.pvlogger.datacollector

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.configuration.DataCollectorDI
import ch.bziswiler.pvlogger.datacollector.configuration.MetricsPusher
import ch.bziswiler.pvlogger.datacollector.configuration.appModule
import ch.bziswiler.pvlogger.datacollector.configuration.initMetrics
import ch.bziswiler.pvlogger.datacollector.configuration.setupConfig
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import io.ktor.server.application.Application // Ktor API (marked)
import io.ktor.server.application.ApplicationStopPreparing // Ktor API (marked)
import io.ktor.server.application.ApplicationStopped // Ktor API (marked)
import io.ktor.server.application.ApplicationStopping // Ktor API (marked)
import io.ktor.server.application.install // Ktor API (marked)
import io.ktor.server.netty.EngineMain // Ktor Netty engine (marked)
import io.ktor.server.plugins.callloging.CallLogging // Ktor plugin (marked)
import mu.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import org.slf4j.event.Level

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) = EngineMain.main(args) // Ktor Netty engine entry point (marked)

fun Application.module(testing: Boolean = false) {
    if (!testing) {
        DataCollectorDI.di = DI {
            import(appModule)
        }
    }
    install(CallLogging) { level = Level.TRACE } // Ktor CallLogging plugin usage (marked)
    setupConfig()
    val registry = initMetrics()
    // Start metrics pusher if enabled via config
    try {
        val pusher: MetricsPusher = DataCollectorDI.di.direct.instance()
        val appConfig: AppConfig = DataCollectorDI.di.direct.instance()
        if (appConfig.metricsPushConfig.enabled) {
            pusher.start()
            Runtime.getRuntime().addShutdownHook(Thread { pusher.stop(deleteFromGateway = true) })
        }
    } catch (e: Exception) {
        logger.warn(e) { "Failed to start MetricsPusher" }
    }
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

fun startServices() {
    val composer: MqReceiverComposer = DataCollectorDI.di.direct.instance()
    composer.compose()
}
