package ch.bziswiler.pvlogger.dataaccessor

import ch.bziswiler.pvlogger.dataaccessor.di.appModule
import ch.bziswiler.pvlogger.dataaccessor.plugins.configureDatabases
import ch.bziswiler.pvlogger.dataaccessor.plugins.configureMonitoring
import ch.bziswiler.pvlogger.dataaccessor.plugins.configureRouting
import ch.bziswiler.pvlogger.dataaccessor.plugins.configureSerialization
import ch.bziswiler.pvlogger.dataaccessor.plugins.setupConfig
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.MqListener
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import org.koin.ktor.ext.inject
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
    val mqListener by inject<MqListener>()
    println(mqListener)
}
