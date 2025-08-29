package ch.bziswiler.pvlogger.datacollector.configuration

import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModbusTcpConfig

        if (port != other.port) return false
        if (delay != other.delay) return false
        if (host != other.host) return false
        if (!meterDeviceIds.contentEquals(other.meterDeviceIds)) return false
        if (!inverterDeviceIds.contentEquals(other.inverterDeviceIds)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = port
        result = 31 * result + delay.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + meterDeviceIds.contentHashCode()
        result = 31 * result + inverterDeviceIds.contentHashCode()
        return result
    }
}

fun setupConfig() {
    val appConfig: AppConfig = DataCollectorDI.di.direct.instance()
    val config = ConfigFactory.load()
    readServerConfig(config, appConfig)
    readModbusConfig(config, appConfig)
    readMqConfig(config, appConfig)
    readMdConfig(config, appConfig)
}

private fun readMdConfig(config: Config, appConfig: AppConfig) = with(config.getConfig("pvlogger.masterdata")) {
    appConfig.apply {
        masterDataConfig = MasterDataConfig(
            host = getString("host"),
            port = getInt("port")
        )
    }
}

private fun readServerConfig(config: Config, appConfig: AppConfig) = with(config.getConfig("ktor.server")) {
    appConfig.apply { serverConfig = ServerConfig(isProd = getBoolean("isProd")) }
}

private fun readMqConfig(config: Config, appConfig: AppConfig) = with(config.getConfig("messagequeue")) {
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
private fun readModbusConfig(config: Config, appConfig: AppConfig) = with(config.getConfig("huawei.modbus.tcp")) {
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
