package ch.bziswiler.pvlogger.datacollector.configuration

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import org.kodein.di.instance

fun setupConfig() {
    val configBinder = ConfigLoaderBuilder.default()
        .addResourceSource("/application-dev.yml")
        .addResourceSource("/application-prod.yml")
        .build()
        .configBinder()
    val config = configBinder.bindOrThrow<AppConfig>("/application-dev.yaml")
    val appConfig by di.instance<AppConfig>()
    appConfig.masterData = config.masterData
    appConfig.modBus = config.modBus
}

class AppConfig {
    lateinit var masterData: MasterDataConfig
    lateinit var modBus: ModbusTcpConfig
}

data class MasterDataConfig(
    val host: String,
    val port: Int
)

@OptIn(ExperimentalUnsignedTypes::class)
data class ModbusTcpConfig(
    val host: String,
    val port: Int,
    val meterDeviceIds: UByteArray,
    val inverterDeviceIds: UByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModbusTcpConfig

        if (port != other.port) return false
        if (host != other.host) return false
        if (!meterDeviceIds.contentEquals(other.meterDeviceIds)) return false
        if (!inverterDeviceIds.contentEquals(other.inverterDeviceIds)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = port
        result = 31 * result + host.hashCode()
        result = 31 * result + meterDeviceIds.contentHashCode()
        result = 31 * result + inverterDeviceIds.contentHashCode()
        return result
    }
}
