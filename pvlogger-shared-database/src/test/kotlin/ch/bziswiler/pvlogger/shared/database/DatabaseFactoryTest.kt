package ch.bziswiler.pvlogger.shared.database

import ch.bziswiler.pvlogger.shared.configuration.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// The services must start as an application role, not only as the postgres
// superuser (backlog 40, infrastructure ADR-0022). No database is named after
// any of these roles, which is what broke the old bootstrap.
class DatabaseFactoryTest {
    @Test
    fun `a role with CREATEDB creates its missing database and migrates it`() {
        superuser("CREATE ROLE creator LOGIN CREATEDB PASSWORD 'pw'")

        DatabaseFactory(config(user = "creator", dbName = "created_db")).use { it.connect() }

        assertEquals("creator", probeTableOwner("created_db"))
    }

    @Test
    fun `a role without CREATEDB starts on a database it owns`() {
        // The shape after a CloudNativePG import: database and tables exist, owned by the app role.
        superuser("CREATE ROLE app LOGIN PASSWORD 'pw'")
        superuser("CREATE DATABASE app_db OWNER app")

        DatabaseFactory(config(user = "app", dbName = "app_db")).use { it.connect() }

        assertEquals("app", probeTableOwner("app_db"))
    }

    @Test
    fun `a missing database the role cannot create fails at once with a clear message`() {
        superuser("CREATE ROLE plain LOGIN PASSWORD 'pw'")
        val started = System.nanoTime()

        val error =
            assertFailsWith<IllegalStateException> {
                DatabaseFactory(config(user = "plain", dbName = "missing_db")).use { it.connect() }
            }

        assertContains(error.message!!, "cannot create")
        // The connect loop waits 15 s between attempts; a missing database must not be retried.
        assertTrue(System.nanoTime() - started < 10_000_000_000, "should fail without retrying")
    }

    @Test
    fun `the TLS mode reaches the driver - require is refused by a server without TLS`() {
        // The test server, like today's central server, has no TLS. "prefer" (every other
        // test here) falls back to plaintext; "require" must not.
        val error =
            assertFailsWith<SQLException> {
                DriverManager.getConnection(config(postgres.username, "postgres", "require").maintenanceUrl, postgres.username, postgres.password)
            }

        assertContains(error.message!!, "SSL")
    }

    @Test
    fun `a pooled connection gives up on a read that outlasts the socket timeout`() {
        // Stands in for a connection to a vanished primary (backlog 45): the server
        // stays silent, and the call must fail instead of blocking for ~15 min.
        superuser("CREATE ROLE sleeper LOGIN CREATEDB PASSWORD 'pw'")

        DatabaseFactory(config(user = "sleeper", dbName = "sleeper_db", socketTimeoutSeconds = 1)).use {
            it.connect()
            val started = System.nanoTime()

            val error = assertFailsWith<Exception> { transaction { exec("SELECT pg_sleep(10)") } }

            assertTrue(System.nanoTime() - started < 5_000_000_000, "should fail at the socket timeout, not after the sleep")
            assertContains(generateSequence<Throwable>(error) { it.cause }.joinToString { it.message.orEmpty() }, "I/O error")
        }
    }

    private fun config(
        user: String,
        dbName: String,
        sslMode: String = DatabaseConfig.DEFAULT_SSL_MODE,
        socketTimeoutSeconds: Int = DatabaseConfig.DEFAULT_SOCKET_TIMEOUT_SECONDS,
    ) = DatabaseConfig(
        driverClass = "org.postgresql.Driver",
        dbServerUrl = serverUrl(),
        dbName = dbName,
        user = user,
        password = "pw",
        maxPoolSize = 1,
        sslMode = sslMode,
        socketTimeoutSeconds = socketTimeoutSeconds,
    )

    private fun probeTableOwner(dbName: String): String =
        DriverManager.getConnection(serverUrl() + dbName, postgres.username, postgres.password).use { connection ->
            connection.createStatement().executeQuery("SELECT tableowner FROM pg_tables WHERE tablename = 'probe'").use {
                it.next()
                it.getString(1)
            }
        }

    private fun DatabaseFactory.use(block: (DatabaseFactory) -> Unit) =
        try {
            block(this)
        } finally {
            close()
        }

    companion object {
        private val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:18").apply { start() }

        private fun serverUrl() = "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/"

        private fun superuser(sql: String) =
            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use {
                it.createStatement().execute(sql)
            }
    }
}
