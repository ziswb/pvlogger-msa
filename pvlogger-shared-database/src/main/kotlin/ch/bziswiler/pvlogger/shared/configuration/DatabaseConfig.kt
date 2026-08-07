package ch.bziswiler.pvlogger.shared.configuration

data class DatabaseConfig(
    val driverClass: String,
    val driverDelegateClass: String,
    val dbServerUrl: String,
    val dbName: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
    val dbUrlParams: String = "",
    // Derived so the database the exists-check/create step targets is always the
    // one the connection uses. Override only where the URL scheme embeds the DB
    // name differently (H2 in-memory).
    val dbServerCanonicalUrl: String = dbServerUrl + dbName + dbUrlParams,
)
