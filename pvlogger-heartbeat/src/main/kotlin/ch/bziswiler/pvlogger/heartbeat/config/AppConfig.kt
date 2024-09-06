package ch.bziswiler.pvlogger.heartbeat.config

import ch.bziswiler.pvlogger.shared.messaging.meta.MqConfig

class AppConfig {
    lateinit var serverConfig: ServerConfig
    lateinit var mqConfig: MqConfig
    lateinit var databaseConfig: DatabaseConfig
    lateinit var jobConfig: JobConfig
}

data class ServerConfig(
    val isProd: Boolean,
)

data class DatabaseConfig(
    val driverClass: String,
    val driverDelegateClass: String,
    val dbServerUrl: String,
    val dbName: String,
    val dbServerCanonicalUrl: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
)

data class JobConfig(
    var lateEvening: String
)