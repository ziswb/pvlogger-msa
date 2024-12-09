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
import io.ktor.server.netty.EngineMain
import io.micrometer.core.instrument.Metrics.addRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.koin.core.context.startKoin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    EngineMain.main(args)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module(testing: Boolean = false) {
    if (!testing) {
        startKoin {
            slf4jLogger()
            modules(appModule)
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
}
