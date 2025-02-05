package ch.bziswiler.pvlogger.kpi

import ch.bziswiler.pvlogger.kpi.configuration.appModule
import ch.bziswiler.pvlogger.kpi.configuration.setupConfig
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
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
    startServices()
}

fun Application.startServices() {
    val composer by inject<MqReceiverComposer>()
    composer.compose()
}
