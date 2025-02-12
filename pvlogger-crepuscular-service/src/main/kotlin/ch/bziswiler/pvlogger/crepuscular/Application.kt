package ch.bziswiler.pvlogger.crepuscular

import ch.bziswiler.pvlogger.crepuscular.configuration.appModule
import ch.bziswiler.pvlogger.crepuscular.configuration.configureDatabase
import ch.bziswiler.pvlogger.crepuscular.configuration.configureMonitoring
import ch.bziswiler.pvlogger.crepuscular.configuration.configureRouting
import ch.bziswiler.pvlogger.crepuscular.configuration.configureSerialization
import ch.bziswiler.pvlogger.crepuscular.configuration.setupConfig
import ch.bziswiler.pvlogger.crepuscular.service.SunriseSunsetService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.event.Level

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
    configureDatabase()
    configureSerialization()
    val service: SunriseSunsetService by inject<SunriseSunsetService>()
    configureRouting(service)
    configureMonitoring()
}
