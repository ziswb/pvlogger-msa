package ch.bziswiler.pvlogger.datacollector

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.configuration.DataCollectorDI
import ch.bziswiler.pvlogger.datacollector.configuration.MetricsPusher
import ch.bziswiler.pvlogger.datacollector.configuration.appModule
import ch.bziswiler.pvlogger.datacollector.configuration.initMetrics
import ch.bziswiler.pvlogger.datacollector.configuration.setupConfig
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import mu.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    // Pure Kotlin entry point (no Ktor)
    DataCollectorDI.di = DI { import(appModule) }

    // Read configuration and initialize metrics
    setupConfig()
    initMetrics()

    // Start metrics pusher if enabled via config
    try {
        val pusher: MetricsPusher = DataCollectorDI.di.direct.instance()
        val appConfig: AppConfig = DataCollectorDI.di.direct.instance()
        if (appConfig.metricsPushConfig.enabled) {
            pusher.start()
            Runtime.getRuntime().addShutdownHook(Thread { pusher.stop(deleteFromGateway = true) })
        } else {
            logger.info { "Metrics Pushgateway pusher disabled by configuration" }
        }
    } catch (e: Exception) {
        logger.warn(e) { "Failed to start MetricsPusher" }
    }

    // Start application services (message receivers, jobs)
    startServices()

    // Keep JVM alive if needed (services typically block via JMS threads). Nothing else required here.
}

fun startServices() {
    val composer: MqReceiverComposer = DataCollectorDI.di.direct.instance()
    composer.compose()
}
