package ch.bziswiler.pvlogger.masterdata.modbus.testutils

import ch.bziswiler.pvlogger.masterdata.server.configuration.appModule
import ch.bziswiler.pvlogger.masterdata.server.module
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import java.sql.SQLException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseApplicationTest : KoinTest {
    @JvmField
    @RegisterExtension
    @Suppress("unused")
    val koinTestExtension = KoinTestExtension.create { modules(appModule) }

    fun <R> withApp(applicationTestBuilder: suspend ApplicationTestBuilder.() -> R) =
        testApplication {
            truncateAllTables()
            application {
                module(testing = true)
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
                    )
            }
            applicationTestBuilder.invoke(this)
        }

    companion object {
        private const val TEST_DB = "pvmasterdata"

        // One PostgreSQL per test JVM (matching the version run in production);
        // DatabaseFactory self-provisions the database on first connect and Flyway
        // migrates it once (see ADR-0009).
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:18").apply { start() }

        private fun PostgreSQLContainer<*>.serverUrl() =
            "jdbc:postgresql://$host:${getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/"

        // Restores fresh-database semantics between tests. On the very first test
        // the database does not exist yet — nothing to clean.
        private fun truncateAllTables() {
            try {
                DriverManager.getConnection("${postgres.serverUrl()}$TEST_DB", postgres.username, postgres.password).use {
                    it.createStatement().execute("TRUNCATE TABLE modbus_register_master_data RESTART IDENTITY CASCADE")
                }
            } catch (ignored: SQLException) {
                // database not created yet — first test in this JVM
            }
        }
    }
}
