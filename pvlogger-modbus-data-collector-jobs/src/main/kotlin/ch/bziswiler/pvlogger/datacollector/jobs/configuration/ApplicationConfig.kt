package ch.bziswiler.pvlogger.datacollector.jobs.configuration

import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File

object AppConfig {
    private val externalPaths =
        listOfNotNull(
            System.getProperty("config.file"), // -Dconfig.file=/path/file.conf
            System.getenv("CONFIG_FILE"), // or CONFIG_FILE=/path/file.conf
            "/config/application.conf", // K8s mount default
        )

    fun load(): Config {
        val defaults = ConfigFactory.parseResources("application.conf")
        val externals =
            externalPaths
                .mapNotNull { path -> File(path).takeIf { it.exists() }?.let(ConfigFactory::parseFile) }
                // first in list should win, so fold with withFallback in left-to-right order
                .fold(ConfigFactory.empty()) { acc, c -> c.withFallback(acc) }

        // Precedence: sys props > env > external files > defaults
        return ConfigFactory
            .systemProperties()
            .withFallback(ConfigFactory.systemEnvironment())
            .withFallback(externals)
            .withFallback(defaults)
            .resolve()
    }
}

data class Settings(
    val masterDataConfig: MasterDataConfig,
    val serverConfig: ServerConfig,
    val modbusTcpConfig: ModbusTcpConfig,
    val mqConfig: MqConfig,
    val metricsPushConfig: MetricsPushConfig,
)

data class MasterDataConfig(
    val host: String,
    val port: Int,
)

data class ServerConfig(
    val isProd: Boolean,
)

data class MetricsPushConfig(
    val enabled: Boolean,
    val url: String,
    val job: String,
    val intervalSeconds: Long,
    val grouping: Map<String, String>,
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
