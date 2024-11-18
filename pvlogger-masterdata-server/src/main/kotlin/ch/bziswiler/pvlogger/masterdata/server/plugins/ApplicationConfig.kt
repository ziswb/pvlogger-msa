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
    with(environment.config.config("masterdata.import")) {
        appConfig.apply {
            masterDataImportConfig = MasterDataImportConfig(
                meterMasterDataPath = property("meterMasterDataPath").getString(),
                inverterMasterDataPath = property("inverterMasterDataPath").getString(),
                deviceIdentifiersMasterDataPath = property("deviceIdentifiersMasterDataPath").getString(),
            )
        }
    }
}

private fun Application.readDbConfig(appConfig: AppConfig) {
    with(environment.config.config("database")) {
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
}

private fun Application.readServerConfig(appConfig: AppConfig) {
    with(environment.config.config("ktor.server")) {
        appConfig.apply { serverConfig = ServerConfig(property("isProd").getString().toBoolean()) }
    }
}
