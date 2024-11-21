package ch.bziswiler.pvlogger.kpi.configuration

import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfigValue
import org.koin.ktor.ext.inject

private const val DELIMITER = ","

class AppConfig {
    lateinit var masterDataConfig: MasterDataConfig
    lateinit var serverConfig: ServerConfig
    lateinit var modbusDataAccessConfig: ModbusDataAccessConfig
    lateinit var mqConfig: MqConfig
}

data class MasterDataConfig(val host: String, val port: Int)

data class ServerConfig(val isProd: Boolean)

@OptIn(ExperimentalUnsignedTypes::class)
data class ModbusDataAccessConfig(
    val host: String,
    val port: Int,
    val meterDeviceIds: UByteArray,
    val inverterDeviceIds: UByteArray
)

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readModbusDataAccessConfig(appConfig)
    readMqConfig(appConfig)
    readMdConfig(appConfig)
}

private fun Application.readMdConfig(appConfig: AppConfig) = with(environment.config.config("pvlogger.masterdata")) {
    appConfig.apply {
        masterDataConfig = MasterDataConfig(
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

@OptIn(ExperimentalUnsignedTypes::class)
private fun Application.readModbusDataAccessConfig(appConfig: AppConfig) = with(environment.config.config("pvlogger.dataaccessor")) {
    appConfig.apply {
        modbusDataAccessConfig = ModbusDataAccessConfig(
            host = property("host").getString(),
            port = property("port").getString().toInt(),
            meterDeviceIds = toUByteArray(property("meterDeviceIds")),
            inverterDeviceIds = toUByteArray(property("inverterDeviceIds"))
        )
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun toUByteArray(configValue: ApplicationConfigValue) =
    configValue.getString().split(DELIMITER).map { it.trim().toUByte() }.toUByteArray()
