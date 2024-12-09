package ch.bziswiler.pvlogger.frontend.configuration

import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

class AppConfig {
    lateinit var dataAccessorConfig: DataAccessorConfig
    lateinit var serverConfig: ServerConfig
    lateinit var mqConfig: MqConfig
}

data class DataAccessorConfig(
    val host: String,
    val port: Int
)

data class ServerConfig(
    val isProd: Boolean,
)

fun Application.readConfiguration() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readMqConfig(appConfig)
    readMdConfig(appConfig)
}

private fun Application.readMdConfig(appConfig: AppConfig) = with(environment.config.config("pvlogger.dataaccessor")) {
    appConfig.apply {
        dataAccessorConfig = DataAccessorConfig(
            host = property("host").getString(),
            port = property("port").getString().toInt()
        )
    }
}

private fun Application.readServerConfig(appConfig: AppConfig) = with(environment.config.config("ktor.server")) {
    appConfig.apply { serverConfig = ServerConfig(isProd = property("isProd").getString().toBoolean()) }
}

private fun Application.readMqConfig(appConfig: AppConfig) = with(environment.config.config("messagequeue")) {
    appConfig.apply {
        mqConfig = MqConfig(
            clientId = property("clientId").getString(),
            user = property("user").getString(),
            passwd = property("passwd").getString(),
            brokerUrl = property("brokerUrl").getString()
        )
    }
}
