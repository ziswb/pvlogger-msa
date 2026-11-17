package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.shared.configuration.DatabaseConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

class AppConfig {
    lateinit var databaseConfig: DatabaseConfig
    lateinit var serverConfig: ServerConfig
}

data class ServerConfig(
    val isProd: Boolean,
    /**
     * Dev only (ADR-0025): store a raw KPI row with the client's dateCreated,
     * so the history backfill can write the past. Off by default; only an
     * image built with DEV_ACCEPT_CLIENT_TIMESTAMPS=true (Tilt) or the local
     * dev profile turns it on. In production in-cluster callers must not
     * backdate raw rows.
     */
    val acceptClientTimestamps: Boolean = false,
)

private val logger = KotlinLogging.logger {}

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readDbConfig(appConfig)
}

private fun Application.readServerConfig(appConfig: AppConfig) {
    val acceptClientTimestamps =
        environment.config.propertyOrNull("dev.acceptClientTimestamps")?.getString().toBoolean()
    if (acceptClientTimestamps) {
        logger.warn { "dev.acceptClientTimestamps is on (a dev build): raw KPI rows take the client's dateCreated. Never in production." }
    }
    appConfig.serverConfig = ServerConfig(
        isProd = environment.config.property("ktor.server.isProd").getString().toBoolean(),
        acceptClientTimestamps = acceptClientTimestamps,
    )
}

private fun Application.readDbConfig(appConfig: AppConfig) = with(environment.config.config("database")) {
    appConfig.databaseConfig = DatabaseConfig(
        driverClass = property("driverClass").getString(),
        dbServerUrl = property("dbServerUrl").getString(),
        dbName = property("dbName").getString(),
        user = property("user").getString(),
        password = property("password").getString(),
        maxPoolSize = property("maxPoolSize").getString().toInt(),
        dbUrlParams = propertyOrNull("dbUrlParams")?.getString() ?: "",
        sslMode = propertyOrNull("sslMode")?.getString() ?: DatabaseConfig.DEFAULT_SSL_MODE,
    )
}
