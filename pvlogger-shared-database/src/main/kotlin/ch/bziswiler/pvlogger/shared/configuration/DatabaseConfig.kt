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
    // Read timeout on pooled connections only (backlog 45). A connection to a
    // vanished server (CNPG switchover, node gone, no RST) otherwise blocks a
    // read for ~15 min. Must stay above the longest legitimate query; callers
    // (the collectors) give up after 30 s anyway. Flyway does not get it, so a
    // long migration (ANALYZE, an index build) is not cut off.
    val socketTimeoutSeconds: Int = DEFAULT_SOCKET_TIMEOUT_SECONDS,
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
        // connectTimeout and tcpKeepAlive hold for every connection, Flyway's too (backlog 45).
        val params =
            listOf(dbUrlParams.removePrefix("?"), "sslmode=$sslMode", "connectTimeout=$CONNECT_TIMEOUT_SECONDS", "tcpKeepAlive=true")
                .filter { it.isNotEmpty() }
        return dbServerUrl + database + params.joinToString("&", prefix = "?")
    }

    companion object {
        const val MAINTENANCE_DB = "postgres"
        const val DEFAULT_SSL_MODE = "prefer"
        const val CONNECT_TIMEOUT_SECONDS = 5
        const val DEFAULT_SOCKET_TIMEOUT_SECONDS = 60
        val SSL_MODES = setOf("disable", "allow", "prefer", "require", "verify-ca", "verify-full")
    }
}
