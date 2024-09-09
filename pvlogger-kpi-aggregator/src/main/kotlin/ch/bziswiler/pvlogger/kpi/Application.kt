package ch.bziswiler.pvlogger.kpi

import ch.bziswiler.pvlogger.kpi.di.appModule
import ch.bziswiler.pvlogger.kpi.plugins.setupConfig
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(testing: Boolean = false) {
    if (!testing) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
    }
    setupConfig()
    startServices()
}

fun Application.startServices() {
    val messageReceiver by inject<MessageReceiver>()
    val messageHandler by inject<MessageHandler>()
    messageReceiver.addMessageHandler(messageHandler)
}