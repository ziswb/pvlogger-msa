@file:OptIn(ExperimentalUnsignedTypes::class)

package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

class AppConfig {
    lateinit var databaseConfig: DatabaseConfig
    lateinit var serverConfig: ServerConfig
    lateinit var mqConfig: MqConfig
}

data class DatabaseConfig(
    val driverClass: String,
    val dbServerUrl: String,
    val dbName: String,
    val dbServerCanonicalUrl: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
)

data class ServerConfig(
    val isProd: Boolean,
)

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readDbConfig(appConfig)
    readMqConfig(appConfig)
}

private fun Application.readServerConfig(appConfig: AppConfig) = with(environment.config.config("ktor.server")) {
    appConfig.apply { serverConfig = ServerConfig(isProd = property("isProd").getString().toBoolean()) }
}

private fun Application.readDbConfig(appConfig: AppConfig) = with(environment.config.config("database")) {
    appConfig.databaseConfig = DatabaseConfig(
        driverClass = property("driverClass").getString(),
        dbServerUrl = property("dbServerUrl").getString(),
        dbName = property("dbName").getString(),
        dbServerCanonicalUrl = property("dbServerCanonicalUrl").getString(),
        user = property("user").getString(),
        password = property("password").getString(),
        maxPoolSize = property("maxPoolSize").getString().toInt()
    )
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
