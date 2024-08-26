package ch.bziswiler.pvlogger.masterdata.server

import ch.bziswiler.pvlogger.masterdata.server.di.appModule
import ch.bziswiler.pvlogger.masterdata.server.plugins.configureDatabases
import ch.bziswiler.pvlogger.masterdata.server.plugins.configureMonitoring
import ch.bziswiler.pvlogger.masterdata.server.plugins.configureRouting
import ch.bziswiler.pvlogger.masterdata.server.plugins.configureSerialization
import ch.bziswiler.pvlogger.masterdata.server.plugins.setupConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) = EngineMain.main(args)

@Suppress("unused")
fun Application.module(testing: Boolean = false) {
    if (!testing) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
    }
    setupConfig()
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureRouting()
}
