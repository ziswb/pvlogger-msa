package ch.bziswiler.pvlogger.datacollector.configuration

import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import org.kodein.di.direct
import org.kodein.di.instance

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
    val appConfig: AppConfig = DataCollectorDI.di.direct.instance()
    val config = ConfigFactory.load()
    readServerConfig(config, appConfig)
    readModbusConfig(config, appConfig)
    readMqConfig(config, appConfig)
    readMdConfig(config, appConfig)
}

private fun Application.readMdConfig(config: com.typesafe.config.Config, appConfig: AppConfig) = with(config.getConfig("pvlogger.masterdata")) {
    appConfig.apply {
        masterDataConfig = MasterDataConfig(
            host = getString("host"),
            port = getInt("port")
        )
    }
}

private fun Application.readServerConfig(config: com.typesafe.config.Config, appConfig: AppConfig) = with(config.getConfig("ktor.server")) {
    appConfig.apply { serverConfig = ServerConfig(isProd = getBoolean("isProd")) }
}

private fun Application.readMqConfig(config: com.typesafe.config.Config, appConfig: AppConfig) = with(config.getConfig("messagequeue")) {
    appConfig.apply {
        mqConfig = MqConfig(
            clientId = getString("clientId"),
            user = getString("user"),
            passwd = getString("passwd"),
            brokerUrl = getString("brokerUrl")
        )
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun Application.readModbusConfig(config: com.typesafe.config.Config, appConfig: AppConfig) = with(config.getConfig("huawei.modbus.tcp")) {
    appConfig.apply {
        modbusTcpConfig = ModbusTcpConfig(
            host = getString("host"),
            port = getInt("port"),
            delay = getLong("delay"),
            meterDeviceIds = getString("meterDeviceIds").split(DELIMITER).map { it.trim().toUByte() }.toUByteArray(),
            inverterDeviceIds = getString("inverterDeviceIds").split(DELIMITER).map { it.trim().toUByte() }.toUByteArray()
        )
    }
}
