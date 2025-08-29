package ch.bziswiler.pvlogger.frontend

import ch.bziswiler.pvlogger.frontend.configuration.appModule
import ch.bziswiler.pvlogger.frontend.configuration.readConfiguration
import ch.bziswiler.pvlogger.frontend.plugins.configureCors
import ch.bziswiler.pvlogger.frontend.plugins.configureHTTP
import ch.bziswiler.pvlogger.frontend.plugins.configureLogging
import ch.bziswiler.pvlogger.frontend.plugins.configureMonitoring
import ch.bziswiler.pvlogger.frontend.plugins.configureRouting
import ch.bziswiler.pvlogger.frontend.plugins.configureSerialization
import ch.bziswiler.pvlogger.frontend.plugins.configureWebSockets
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.netty.EngineMain
import io.micrometer.core.instrument.Metrics.addRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import mu.KotlinLogging
import ch.bziswiler.pvlogger.frontend.configuration.FrontendDI
import org.kodein.di.DI

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    EngineMain.main(args)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module(testing: Boolean = false) {
    if (!testing) {
        FrontendDI.di = DI {
            import(appModule)
        }
    }
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    addRegistry(registry)
    readConfiguration()
    configureMonitoring(registry, environment)
    configureWebSockets()
    configureHTTP()
    configureLogging()
    configureSerialization()
    configureRouting(registry)
    configureCors()
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
