package ch.bziswiler.pvlogger.masterdata.server.plugins

import ch.bziswiler.pvlogger.masterdata.server.config.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.config.DatabaseConfig
import ch.bziswiler.pvlogger.masterdata.server.config.MasterDataImportConfig
import ch.bziswiler.pvlogger.masterdata.server.config.ServerConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readDbConfig(appConfig)
    readImportConfig(appConfig)
}

private fun Application.readImportConfig(appConfig: AppConfig) {
    val masterDataObject = environment.config.config("masterdata.import")
    val meterMasterDataPath = masterDataObject.property("meterMasterDataPath").getString()
    val inverterMasterDataPath = masterDataObject.property("inverterMasterDataPath").getString()
    val deviceIdentifiersMasterDataPath = masterDataObject.property("deviceIdentifiersMasterDataPath").getString()
    appConfig.masterDataImportConfig =
        MasterDataImportConfig(
            meterMasterDataPath,
            inverterMasterDataPath,
            deviceIdentifiersMasterDataPath,
        )
}

private fun Application.readDbConfig(appConfig: AppConfig) {
    val databaseObject = environment.config.config("database")
    val driverClass = databaseObject.property("driverClass").getString()
    val dbServerUrl = databaseObject.property("dbServerUrl").getString()
    val dbName = databaseObject.property("dbName").getString()
    val dbCanonicalUrl = databaseObject.property("dbServerCanonicalUrl").getString()
    val user = databaseObject.property("user").getString()
    val password = databaseObject.property("password").getString()
    val maxPoolSize = databaseObject.property("maxPoolSize").getString().toInt()
    appConfig.databaseConfig = DatabaseConfig(
        driverClass,
        dbServerUrl,
        dbName,
        dbCanonicalUrl,
        user,
        password,
        maxPoolSize
    )
}

private fun Application.readServerConfig(appConfig: AppConfig) {
    val serverObject = environment.config.config("ktor.server")
    val isProd = serverObject.property("isProd").getString().toBoolean()
    appConfig.serverConfig = ServerConfig(isProd)
}
