package ch.bziswiler.pvlogger.masterdata.server

import ch.bziswiler.pvlogger.masterdata.server.configuration.appModule
import ch.bziswiler.pvlogger.masterdata.server.configuration.configureDatabases
import ch.bziswiler.pvlogger.masterdata.server.configuration.configureRouting
import ch.bziswiler.pvlogger.masterdata.server.configuration.configureSerialization
import ch.bziswiler.pvlogger.masterdata.server.configuration.setupConfig
import ch.bziswiler.pvlogger.shared.configuration.TimeZones
import ch.bziswiler.pvlogger.shared.ktor.configureMonitoring
import ch.bziswiler.pvlogger.shared.ktor.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    TimeZones.requireConfigured()
    EngineMain.main(args)
}

@Suppress("unused")
fun Application.module(testing: Boolean = false) {
    if (!testing) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
    }
    setupConfig()
    configureStatusPages()
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureRouting()
}
