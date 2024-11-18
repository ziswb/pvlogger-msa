package ch.bziswiler.pvlogger.masterdata.server.configuration

import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

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

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readDbConfig(appConfig)
    readImportConfig(appConfig)
}

private fun Application.readImportConfig(appConfig: AppConfig) = with(environment.config.config("masterdata.import")) {
    appConfig.apply {
        masterDataImportConfig = MasterDataImportConfig(
            meterMasterDataPath = property("meterMasterDataPath").getString(),
            inverterMasterDataPath = property("inverterMasterDataPath").getString(),
            deviceIdentifiersMasterDataPath = property("deviceIdentifiersMasterDataPath").getString(),
        )
    }
}

private fun Application.readDbConfig(appConfig: AppConfig) = with(environment.config.config("database")) {
    appConfig.apply {
        databaseConfig = DatabaseConfig(
            driverClass = property("driverClass").getString(),
            dbServerUrl = property("dbServerUrl").getString(),
            dbName = property("dbName").getString(),
            dbServerCanonicalUrl = property("dbServerCanonicalUrl").getString(),
            user = property("user").getString(),
            password = property("password").getString(),
            maxPoolSize = property("maxPoolSize").getString().toInt()
        )
    }
}

private fun Application.readServerConfig(appConfig: AppConfig) = with(environment.config.config("ktor.server")) {
    appConfig.apply { serverConfig = ServerConfig(property("isProd").getString().toBoolean()) }
}
