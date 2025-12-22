package ch.bziswiler.pvlogger.frontend.configuration

import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

class AppConfig {
    lateinit var dataAccessorConfig: DataAccessorConfig
    lateinit var serverConfig: ServerConfig
}

data class DataAccessorConfig(
    val host: String,
    val port: Int,
)

data class ServerConfig(
    val isProd: Boolean,
)

fun Application.readConfiguration() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readDataAccessorConfig(appConfig)
}

private fun Application.readDataAccessorConfig(appConfig: AppConfig) =
    with(environment.config.config("pvlogger.dataaccessor")) {
        appConfig.apply {
            dataAccessorConfig =
                DataAccessorConfig(
                    host = property("host").getString(),
                    port = property("port").getString().toInt(),
                )
        }
    }

private fun Application.readServerConfig(appConfig: AppConfig) =
    with(environment.config.config("ktor.server")) {
        appConfig.apply { serverConfig = ServerConfig(isProd = property("isProd").getString().toBoolean()) }
    }
