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
    // The database name is stated once — the self-provisioning and the real
    // connection always target the same database (ADR-0008/ADR-0009).
    val dbServerCanonicalUrl: String = dbServerUrl + dbName + dbUrlParams

    // Used only to create a missing database. Named explicitly: a JDBC URL without
    // a database makes the driver pick the one named after the user, which does
    // not exist for an application role.
    val maintenanceUrl: String = dbServerUrl + MAINTENANCE_DB + dbUrlParams

    companion object {
        const val MAINTENANCE_DB = "postgres"
    }
}
