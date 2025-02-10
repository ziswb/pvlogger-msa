package ch.bziswiler.pvlogger.crepuscular

import ch.bziswiler.pvlogger.crepuscular.configuration.appModule
import ch.bziswiler.pvlogger.crepuscular.configuration.configureDatabases
import ch.bziswiler.pvlogger.crepuscular.configuration.configureMonitoring
import ch.bziswiler.pvlogger.crepuscular.configuration.configureRouting
import ch.bziswiler.pvlogger.crepuscular.configuration.setupConfig
import ch.bziswiler.pvlogger.crepuscular.service.SunriseSunsetService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    EngineMain.main(args)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module(testing: Boolean = false) {
    if (!testing) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
    }
    setupConfig()
    configureDatabases()
    val service: SunriseSunsetService by inject<SunriseSunsetService>()
    configureRouting(service)
    configureMonitoring()
}
