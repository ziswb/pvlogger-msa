package ch.bziswiler.pvlogger.datacollector.configuration

private const val DELIMITER = ","

object AppConfig {
    val jobConfig: JobConfig
        get() =
            JobConfig(
                name = System.getenv("JOB_NAME") ?: "MISSING_JOB_NAME",
            )
    val masterDataConfig: MasterDataConfig
        get() =
            MasterDataConfig(
                host = System.getenv("MASTER_DATA_HOST") ?: "localhost",
                port = System.getenv("MASTER_DATA_PORT")?.toInt() ?: 8081,
            )

    @OptIn(ExperimentalUnsignedTypes::class)
    val modbusTcpConfig: ModbusTcpConfig
        get() =
            ModbusTcpConfig(
                host = System.getenv("LWIP_IP") ?: "",
                port = System.getenv("LWIP_PORT")?.toInt() ?: 502,
                delay = System.getenv("LWIP_DELAY")?.toLong() ?: 1L,
                meterDeviceIds = readUByteArray("METER_DEVICE_IDS", "1"),
                inverterDeviceIds = readUByteArray("INVERTER_DEVICE_IDS", "1,16"),
            )
}

data class JobConfig(
    val name: String,
)

data class MasterDataConfig(
    val host: String,
    val port: Int,
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

@OptIn(ExperimentalUnsignedTypes::class)
private fun readUByteArray(
    env: String,
    defaultValue: String,
): UByteArray =
    (System.getenv(env) ?: defaultValue)
        .split(DELIMITER)
        .map {
            it.trim().toUByte()
        }.toUByteArray()
