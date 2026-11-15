package ch.bziswiler.testutils

import ch.bziswiler.pvlogger.dataaccessor.configuration.AppConfig
import ch.bziswiler.pvlogger.dataaccessor.configuration.appModule
import ch.bziswiler.pvlogger.dataaccessor.configureApplication
import ch.bziswiler.pvlogger.shared.database.DatabaseFactory
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager
import java.sql.SQLException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TestBase : KoinTest {
    @JvmField
    @RegisterExtension
    @Suppress("unused")
    val koinTestExtension = KoinTestExtension.create { modules(appModule) }

    /** Runs a test application; [overrides] adds or overrides configuration keys. */
    fun <R> withApp(
        overrides: Map<String, String> = emptyMap(),
        applicationTestBuilder: suspend ApplicationTestBuilder.() -> R,
    ) =
        testApplication {
            truncateAllTables()
            application {
                configureApplication()
                org.koin.dsl.module {
                    single<AppConfig> { AppConfig() }
                    single<DatabaseFactory> { DatabaseFactory(get<AppConfig>().databaseConfig) }
                }
            }
            environment {
                config =
                    MapApplicationConfig(
                        "ktor.server.isProd" to "false",
                        "database.driverClass" to "org.postgresql.Driver",
                        "database.dbServerUrl" to postgres.serverUrl(),
                        "database.dbName" to TEST_DB,
                        "database.user" to postgres.username,
                        "database.password" to postgres.password,
                        "database.maxPoolSize" to "1",
                        "masterdata.import.meterMasterDataPath" to "",
                        "masterdata.import.inverterMasterDataPath" to "",
                        "masterdata.import.deviceIdentifiersMasterDataPath" to "",
                        "messagequeue.clientId" to "pvlogger-modbus-data-accessor",
                        "messagequeue.user" to "artemis",
                        "messagequeue.passwd" to "artemis",
                        "messagequeue.brokerUrl" to "",
                    ).apply { overrides.forEach { (key, value) -> put(key, value) } }
            }
            applicationTestBuilder.invoke(this)
        }

    companion object {
        private const val TEST_DB = "pvdata"

        // One PostgreSQL per test JVM (matching the version run in production).
        // DatabaseFactory self-provisions the pvdata database on first connect and
        // Flyway migrates it exactly once — V8 creates a cluster-wide role and must
        // not run twice, so tests share the database instead of getting one each.
        private val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:18").apply { start() }

        private val tables =
            listOf(
                "house_power_kpi_values",
                "modbus_response_data",
                "modbus_characteristic_curve_data",
                "modbus_register_integer_values",
                "modbus_register_short_values",
                "modbus_register_string_values",
                "modbus_register_uinteger_values",
                "modbus_register_ushort_values",
                "raw_kpi_register_values",
            )

        private fun PostgreSQLContainer.serverUrl() =
            "jdbc:postgresql://$host:${getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/"

        /** Connections to the test database other than the one asking. */
        fun openConnectionsToTestDb(): Int =
            DriverManager.getConnection("${postgres.serverUrl()}$TEST_DB", postgres.username, postgres.password).use {
                it.createStatement().executeQuery(
                    "SELECT count(*) FROM pg_stat_activity WHERE datname = '$TEST_DB' AND pid <> pg_backend_pid()",
                ).use { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            }

        // Restores the fresh-database semantics the previous per-test in-memory H2
        // instance provided. On the very first test the database does not exist yet
        // (DatabaseFactory creates it during application start) — nothing to clean.
        private fun truncateAllTables() {
            try {
                DriverManager.getConnection("${postgres.serverUrl()}$TEST_DB", postgres.username, postgres.password).use {
                    it.createStatement().execute("TRUNCATE TABLE ${tables.joinToString(", ")} RESTART IDENTITY CASCADE")
                }
            } catch (ignored: SQLException) {
                // database not created yet — first test in this JVM
            }
        }
    }
}
