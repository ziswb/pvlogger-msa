package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.shared.configuration.DatabaseConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

class AppConfig {
    lateinit var databaseConfig: DatabaseConfig
    lateinit var serverConfig: ServerConfig
}

data class ServerConfig(
    val isProd: Boolean,
)

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readDbConfig(appConfig)
}

private fun Application.readServerConfig(appConfig: AppConfig) = with(environment.config.config("ktor.server")) {
    appConfig.apply { serverConfig = ServerConfig(isProd = property("isProd").getString().toBoolean()) }
}

private fun Application.readDbConfig(appConfig: AppConfig) = with(environment.config.config("database")) {
    appConfig.databaseConfig = DatabaseConfig(
        driverClass = property("driverClass").getString(),
        driverDelegateClass = "",
        dbServerUrl = property("dbServerUrl").getString(),
        dbName = property("dbName").getString(),
        dbServerCanonicalUrl = property("dbServerCanonicalUrl").getString(),
        user = property("user").getString(),
        password = property("password").getString(),
        maxPoolSize = property("maxPoolSize").getString().toInt()
    )
}
