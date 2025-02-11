package ch.bziswiler.pvlogger.fusionsolar.configuration

import ch.bziswiler.pvlogger.shared.configuration.DatabaseConfig
import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

private const val HUAWEI_FUSION_SOLAR = "huawei.fusion.solar"
private const val BASE_URL_OLD = "baseUrlOld"
private const val BASE_URL_NEW = "baseUrlNew"
private const val USERNAME = "username"
private const val PASSWORD = "password"
private const val USER = "user"
private const val MAX_POOL_SIZE = "maxPoolSize"
private const val BROKER_URL = "brokerUrl"
private const val PASSWD = "passwd"
private const val CLIENT_ID = "clientId"
private const val MESSAGEQUEUE = "messagequeue"
private const val DB_SERVER_CANONICAL_URL = "dbServerCanonicalUrl"
private const val DB_NAME = "dbName"
private const val DB_SERVER_URL = "dbServerUrl"
private const val DRIVER_CLASS = "driverClass"
private const val DATABASE = "database"
private const val KTOR_SERVER = "ktor.server"
private const val IS_PROD = "isProd"

class AppConfig {
    lateinit var databaseConfig: DatabaseConfig
    lateinit var serverConfig: ServerConfig
    lateinit var mqConfig: MqConfig
    lateinit var fusionSolarConfig: FusionSolarConfig
}

data class ServerConfig(
    val isProd: Boolean,
)

data class FusionSolarConfig(
    val username: String,
    val password: String,
    val baseUrlOld: String,
    val baseUrlNew: String
)

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readDbConfig(appConfig)
    readMqConfig(appConfig)
}

private fun Application.readServerConfig(appConfig: AppConfig) = with(environment.config.config(KTOR_SERVER)) {
    appConfig.apply { serverConfig = ServerConfig(isProd = property(IS_PROD).getString().toBoolean()) }
}


private fun Application.readDbConfig(appConfig: AppConfig) = with(environment.config.config(DATABASE)) {
    appConfig.databaseConfig = DatabaseConfig(
        driverClass = property(DRIVER_CLASS).getString(),
        driverDelegateClass = "",
        dbServerUrl = property(DB_SERVER_URL).getString(),
        dbName = property(DB_NAME).getString(),
        dbServerCanonicalUrl = property(DB_SERVER_CANONICAL_URL).getString(),
        user = property(USER).getString(),
        password = property(PASSWORD).getString(),
        maxPoolSize = property(MAX_POOL_SIZE).getString().toInt()
    )
}

private fun Application.readMqConfig(appConfig: AppConfig) = with(environment.config.config(MESSAGEQUEUE)) {
    appConfig.apply {
        mqConfig = MqConfig(
            clientId = property(CLIENT_ID).getString(),
            user = property(USER).getString(),
            passwd = property(PASSWD).getString(),
            brokerUrl = property(BROKER_URL).getString()
        )
    }
}

private fun Application.fusionSolarConfig(appConfig: AppConfig) = with(environment.config.config(HUAWEI_FUSION_SOLAR)) {
    appConfig.apply {
        fusionSolarConfig = FusionSolarConfig(
            username = property(USERNAME).getString(),
            password = property(PASSWORD).getString(),
            baseUrlOld = property(BASE_URL_OLD).getString(),
            baseUrlNew = property(BASE_URL_NEW).getString()
        )
    }
}
