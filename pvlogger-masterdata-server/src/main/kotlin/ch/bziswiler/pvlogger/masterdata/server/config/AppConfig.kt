package ch.bziswiler.pvlogger.masterdata.server.config

class AppConfig {
    lateinit var databaseConfig: DatabaseConfig
    lateinit var serverConfig: ServerConfig
    lateinit var masterDataImportConfig: MasterDataImportConfig
}

data class DatabaseConfig(
    val driverClass: String,
    val dbServerUrl: String,
    val dbName: String,
    val dbServerCanonicalUrl: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
)

data class ServerConfig(
    val isProd: Boolean,
)

data class MasterDataImportConfig(
    val meterMasterDataPath: String,
    val inverterMasterDataPath: String,
    val deviceIdentifiersMasterDataPath: String,
)
