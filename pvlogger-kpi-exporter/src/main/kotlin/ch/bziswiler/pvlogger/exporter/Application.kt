package ch.bziswiler.pvlogger.exporter

import ch.bziswiler.pvlogger.exporter.configuration.appModule
import ch.bziswiler.pvlogger.exporter.configuration.setupConfig
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) = EngineMain.main(args)

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