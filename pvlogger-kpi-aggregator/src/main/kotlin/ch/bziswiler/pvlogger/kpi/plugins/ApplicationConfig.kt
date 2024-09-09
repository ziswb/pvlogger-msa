@file:OptIn(ExperimentalUnsignedTypes::class)

package ch.bziswiler.pvlogger.kpi.plugins

import ch.bziswiler.pvlogger.kpi.config.AppConfig
import ch.bziswiler.pvlogger.kpi.config.ServerConfig
import ch.bziswiler.pvlogger.shared.messaging.meta.MqConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readMqConfig(appConfig)
}

private fun Application.readServerConfig(appConfig: AppConfig) {
    val serverObject = environment.config.config("ktor.server")
    val isProd = serverObject.property("isProd").getString().toBoolean()
    appConfig.serverConfig = ServerConfig(isProd)
}

private fun Application.readMqConfig(appConfig: AppConfig) {
    val mqObject = environment.config.config("messagequeue")
    val clientId = mqObject.property("clientId").getString()
    val user = mqObject.property("user").getString()
    val passwd = mqObject.property("passwd").getString()
    val brokerUrl = mqObject.property("brokerUrl").getString()
    appConfig.mqConfig = MqConfig(clientId, user, passwd, brokerUrl)
}
