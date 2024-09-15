package ch.bziswiler.pvlogger.datacollector.config

import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig

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
