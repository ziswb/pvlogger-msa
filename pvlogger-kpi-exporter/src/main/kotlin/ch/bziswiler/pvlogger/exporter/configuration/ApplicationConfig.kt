package ch.bziswiler.pvlogger.exporter.configuration

import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

class AppConfig {
    lateinit var dataAccessConfig: ModbusDataAccessConfig
    lateinit var serverConfig: ServerConfig
    lateinit var mqConfig: MqConfig
    lateinit var exportConfig: ExportConfig
}

data class ServerConfig(val isProd: Boolean)

data class ModbusDataAccessConfig(
    val host: String,
    val port: Int
)

data class ExportConfig(
    val root: String,
    val path: String,
    val name: String,
)

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readModbusDataAccessConfig(appConfig)
    readServerConfig(appConfig)
    readMqConfig(appConfig)
    readExportConfig(appConfig)
}

private fun Application.readModbusDataAccessConfig(appConfig: AppConfig) = with(environment.config.config("pvlogger.dataaccessor")) {
    appConfig.apply {
        dataAccessConfig = ModbusDataAccessConfig(
            host = property("host").getString(),
            port = property("port").getString().toInt(),
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

private fun Application.readExportConfig(appConfig: AppConfig) = with(environment.config.config("pvlogger.export")) {
    appConfig.apply {
        exportConfig = ExportConfig(
            root = property("root").getString(),
            path = property("path").getString(),
            name = property("name").getString(),
        )
    }
}
