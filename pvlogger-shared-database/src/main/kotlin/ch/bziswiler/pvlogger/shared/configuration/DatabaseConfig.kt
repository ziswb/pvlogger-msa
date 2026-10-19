package ch.bziswiler.pvlogger.shared.configuration

data class DatabaseConfig(
    val driverClass: String,
    val dbServerUrl: String,
    val dbName: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
    val dbUrlParams: String = "",
    // TLS to the server (security finding 3). "prefer" tries TLS and falls back to
    // plaintext, so it works against a server without TLS; set "require" once the
    // server has it (backlog 40).
    val sslMode: String = DEFAULT_SSL_MODE,
) {
    init {
        require(sslMode in SSL_MODES) { "sslMode must be one of $SSL_MODES, was '$sslMode'" }
    }

    // The database name is stated once — the self-provisioning and the real
    // connection always target the same database (ADR-0008/ADR-0009).
    val dbServerCanonicalUrl: String = urlFor(dbName)

    // Used only to create a missing database. Named explicitly: a JDBC URL without
    // a database makes the driver pick the one named after the user, which does
    // not exist for an application role.
    val maintenanceUrl: String = urlFor(MAINTENANCE_DB)

    private fun urlFor(database: String): String {
        val params = listOf(dbUrlParams.removePrefix("?"), "sslmode=$sslMode").filter { it.isNotEmpty() }
        return dbServerUrl + database + params.joinToString("&", prefix = "?")
    }

    companion object {
        const val MAINTENANCE_DB = "postgres"
        const val DEFAULT_SSL_MODE = "prefer"
        val SSL_MODES = setOf("disable", "allow", "prefer", "require", "verify-ca", "verify-full")
    }
}
