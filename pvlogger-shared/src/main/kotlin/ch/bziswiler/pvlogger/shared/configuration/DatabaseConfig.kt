package ch.bziswiler.pvlogger.shared.configuration

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