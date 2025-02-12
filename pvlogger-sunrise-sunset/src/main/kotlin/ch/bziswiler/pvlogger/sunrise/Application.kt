package ch.bziswiler.pvlogger.sunrise

import ch.bziswiler.pvlogger.sunrise.configuration.appModule
import ch.bziswiler.pvlogger.sunrise.configuration.configureDatabase
import ch.bziswiler.pvlogger.sunrise.configuration.configureMonitoring
import ch.bziswiler.pvlogger.sunrise.configuration.configureRouting
import ch.bziswiler.pvlogger.sunrise.configuration.configureSerialization
import ch.bziswiler.pvlogger.sunrise.configuration.setupConfig
import ch.bziswiler.pvlogger.sunrise.service.SunriseSunsetService
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
