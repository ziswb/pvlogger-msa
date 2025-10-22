package ch.bziswiler.pvlogger.datacollector.jobs

import ch.bziswiler.pvlogger.datacollector.jobs.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.jobs.configuration.DataCollectorDI
import ch.bziswiler.pvlogger.datacollector.jobs.configuration.MetricsPusher
import ch.bziswiler.pvlogger.datacollector.jobs.configuration.Settings
import ch.bziswiler.pvlogger.datacollector.jobs.configuration.appModule
import ch.bziswiler.pvlogger.datacollector.jobs.configuration.initMetrics
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import io.github.config4k.extract
import mu.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    initDependencyInjection()
    initMetrics()
    val settings = AppConfig.load().extract<Settings>()
    try {
        val pusher: MetricsPusher = DataCollectorDI.di.direct.instance()
        if (settings.metricsPushConfig.enabled) {
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

private fun initDependencyInjection() {
    DataCollectorDI.di = DI { import(appModule) }
}

fun startServices() {
    val composer: MqReceiverComposer = DataCollectorDI.di.direct.instance()
    composer.compose()
}
