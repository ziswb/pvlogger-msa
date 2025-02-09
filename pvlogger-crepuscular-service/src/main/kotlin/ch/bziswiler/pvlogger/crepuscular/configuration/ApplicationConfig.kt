package ch.bziswiler.pvlogger.crepuscular.configuration

import ch.bziswiler.pvlogger.shared.configuration.DatabaseConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

class AppConfig {
    lateinit var databaseConfig: DatabaseConfig
    lateinit var serverConfig: ServerConfig
    lateinit var sunriseSunsetServiceConfig: SunriseSunsetServiceConfig
}

data class ServerConfig(
    val isProd: Boolean,
)

data class SunriseSunsetServiceConfig(
    val host: String,
    val port: Int,
    val path: String,
    val latitude: String,
    val longitude: String,
)

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readDbConfig(appConfig)
    readSunriseSunsetServiceConfig(appConfig)
}

private fun Application.readSunriseSunsetServiceConfig(appConfig: AppConfig) = with(environment.config.config("sunset-sunrise-service")) {
    appConfig.apply {
        sunriseSunsetServiceConfig = SunriseSunsetServiceConfig(
            host = property("host").getString(),
            port = property("port").getString().toInt(),
            path = property("path").getString(),
            latitude = property("latitude").getString(),
            longitude = property("longitude").getString()
        )
    }
}

private fun Application.readDbConfig(appConfig: AppConfig) = with(environment.config.config("database")) {
    appConfig.apply {
        databaseConfig = DatabaseConfig(
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
}

private fun Application.readServerConfig(appConfig: AppConfig) = with(environment.config.config("ktor.server")) {
    appConfig.apply { serverConfig = ServerConfig(property("isProd").getString().toBoolean()) }
}
