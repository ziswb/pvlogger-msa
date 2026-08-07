package ch.bziswiler.pvlogger.shared.configuration

data class DatabaseConfig(
    val driverClass: String,
    val dbServerUrl: String,
    val dbName: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
    val dbUrlParams: String = "",
) {
    // The database name is stated once — the self-provisioning exists-check and
    // the real connection always target the same database (ADR-0008/ADR-0009).
    val dbServerCanonicalUrl: String = dbServerUrl + dbName + dbUrlParams
}
