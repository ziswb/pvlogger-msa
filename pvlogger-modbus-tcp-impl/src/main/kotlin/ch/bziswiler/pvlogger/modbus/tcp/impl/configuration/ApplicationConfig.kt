package ch.bziswiler.pvlogger.modbus.tcp.impl.configuration

import java.util.*

private const val DELIMITER = ","

class AppConfig {
    lateinit var masterDataConfig: MasterDataConfig
    lateinit var modbusTcpConfig: ModbusTcpConfig
}

data class MasterDataConfig(
    val host: String,
    val port: Int
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

class ApplicationConfig {

    private val properties = Properties()

    fun readConfig() {
        properties.load(this.javaClass.classLoader.getResourceAsStream(""))
        val appConfig = AppConfig()
        readModbusConfig(appConfig)
        readMdConfig(appConfig)
    }

    private fun readMdConfig(appConfig: AppConfig) = appConfig.apply {
        masterDataConfig = MasterDataConfig(
            host = properties.getProperty("pvlogger.masterdata.host"),
            port = properties.getProperty("pvlogger.masterdata.port").toInt()
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun readModbusConfig(appConfig: AppConfig) = appConfig.apply {
        modbusTcpConfig = ModbusTcpConfig(
            host = properties.getProperty("huawei.modbus.tcp.host"),
            port = properties.getProperty("huawei.modbus.tcp.port").toInt(),
            delay = properties.getProperty("huawei.modbus.tcp.delay").toLong(),
            meterDeviceIds = properties
                .getProperty("huawei.modbus.tcp.meterDeviceIds")
                .split(DELIMITER)
                .map { it.trim().toUByte() }
                .toUByteArray(),
            inverterDeviceIds = properties
                .getProperty("huawei.modbus.tcp.inverterDeviceIds")
                .split(DELIMITER)
                .map { it.trim().toUByte() }
                .toUByteArray()
        )
    }
}
