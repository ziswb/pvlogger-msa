@file:OptIn(ExperimentalUnsignedTypes::class)

package ch.bziswiler.pvlogger.datacollector.configuration

import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

private const val DELIMITER = ","

class AppConfig {
    lateinit var masterDataConfig: MasterDataConfig
    lateinit var serverConfig: ServerConfig
    lateinit var modbusTcpConfig: ModbusTcpConfig
    lateinit var mqConfig: MqConfig
}

data class MasterDataConfig(
    val host: String,
    val port: Int
)

data class ServerConfig(
    val isProd: Boolean,
)

@OptIn(ExperimentalUnsignedTypes::class)
data class ModbusTcpConfig(
    val host: String,
    val port: Int,
    val delay: Long,
    val meterDeviceIds: UByteArray,
    val inverterDeviceIds: UByteArray,
)

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readModbusConfig(appConfig)
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

@ExperimentalUnsignedTypes
private fun Application.readModbusConfig(appConfig: AppConfig) = with(environment.config.config("huawei.modbus.tcp")) {
    appConfig.apply {
        modbusTcpConfig = ModbusTcpConfig(
            host = property("host").getString(),
            port = property("port").getString().toInt(),
            delay = property("delay").getString().toLong(),
            meterDeviceIds = property("meterDeviceIds").getString().split(DELIMITER).map { it.trim().toUByte() }.toUByteArray(),
            inverterDeviceIds = property("inverterDeviceIds").getString().split(DELIMITER).map { it.trim().toUByte() }.toUByteArray()
        )
    }
}
