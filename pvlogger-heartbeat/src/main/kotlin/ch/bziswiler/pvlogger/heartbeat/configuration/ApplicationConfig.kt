package ch.bziswiler.pvlogger.heartbeat.configuration

import ch.bziswiler.pvlogger.shared.configuration.DatabaseConfig
import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

class AppConfig {
    lateinit var serverConfig: ServerConfig
    lateinit var mqConfig: MqConfig
    lateinit var databaseConfig: DatabaseConfig
    lateinit var jobConfig: JobConfig
}

data class ServerConfig(
    val isProd: Boolean,
)

data class JobConfig(
    var lateEvening: String
)

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readDbConfig(appConfig)
    readMqConfig(appConfig)
    readJobConfig(appConfig)
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

private fun Application.readDbConfig(appConfig: AppConfig) = with(environment.config.config("database")) {
    appConfig.apply {
        databaseConfig = DatabaseConfig(
            driverClass = property("driverClass").getString(),
            driverDelegateClass = property("driverDelegateClass").getString(),
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
    appConfig.apply { serverConfig = ServerConfig(isProd = property("isProd").getString().toBoolean()) }
}

private fun Application.readJobConfig(appConfig: AppConfig) = with(environment.config.config("job")) {
    appConfig.apply { jobConfig = JobConfig(lateEvening = property("lateEvening").getString()) }
}

