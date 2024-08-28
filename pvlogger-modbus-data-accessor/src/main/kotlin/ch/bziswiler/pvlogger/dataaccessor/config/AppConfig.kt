package ch.bziswiler.pvlogger.dataaccessor.config

class AppConfig {
    lateinit var databaseConfig: DatabaseConfig
    lateinit var serverConfig: ServerConfig
    lateinit var mqConfig: MqConfig
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

data class MqConfig(
    val clientId: String,
    val user: String,
    val passwd: String,
    val brokerUrl: String
)