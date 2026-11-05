package ch.bziswiler.pvlogger.shared.database

import ch.bziswiler.pvlogger.shared.configuration.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway.configure
import org.jetbrains.exposed.v1.jdbc.Database.Companion.connect
import java.lang.Thread.sleep
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicInteger

class DatabaseFactory(
    private val dbConfig: DatabaseConfig,
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val maxRetries = 30
    private val sleepMs: Long = 15000
    private val retryCounter = AtomicInteger(0)
    private var datasource: HikariDataSource? = null

    // Connects to the target database first, so a normal start needs no rights
    // beyond that database: an application role works, not only a superuser
    // (backlog 40). Only when the database is missing does it go through the
    // maintenance database to create it, which needs CREATEDB.
    fun connect() {
        datasource =
            try {
                hikari(dbConfig.dbServerCanonicalUrl)
            } catch (e: DatabaseMissingException) {
                createDatabase()
                hikari(dbConfig.dbServerCanonicalUrl)
            }
        connect(datasource!!)
        migrateDatabase()
    }

    // Closes the connection pool. Services register this for shutdown
    // (closeOnShutdown in pvlogger-shared-ktor, security finding 29).
    override fun close() {
        datasource?.close()
    }

    private fun createDatabase() {
        logger.info("Database {} does not exist, creating it", dbConfig.dbName)
        try {
            DriverManager.getConnection(dbConfig.maintenanceUrl, dbConfig.user, dbConfig.password).use { connection ->
                connection.autoCommit = true
                connection.createStatement().use { it.execute("CREATE DATABASE \"${dbConfig.dbName}\"") }
            }
        } catch (e: SQLException) {
            throw IllegalStateException(
                "Database ${dbConfig.dbName} does not exist and user ${dbConfig.user} cannot create it " +
                    "(needs CREATEDB and access to the ${DatabaseConfig.MAINTENANCE_DB} database)",
                e,
            )
        }
    }

    private fun migrateDatabase() {
        configure().dataSource(dbConfig.dbServerCanonicalUrl, dbConfig.user, dbConfig.password).load().migrate()
    }

    private fun hikari(dbServerUrl: String): HikariDataSource {
        val config =
            HikariConfig().apply {
                driverClassName = dbConfig.driverClass
                jdbcUrl = dbServerUrl
                username = dbConfig.user
                password = dbConfig.password
                maximumPoolSize = dbConfig.maxPoolSize
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                // Fail fast on dead connections so the pool replaces them (backlog 45):
                // a read on one ends after socketTimeout, a borrowed idle one is checked
                // within validationTimeout, idle ones are probed every keepaliveTime, and
                // a caller waits at most connectionTimeout for a connection.
                // As a String: pgjdbc reads it with Properties.getProperty, which ignores other types.
                addDataSourceProperty("socketTimeout", dbConfig.socketTimeoutSeconds.toString())
                connectionTimeout = 10_000
                validationTimeout = 3_000
                keepaliveTime = 30_000
                validate()
            }
        return tryConnectingToDatabase(config)
    }

    private fun tryConnectingToDatabase(config: HikariConfig): HikariDataSource {
        retryCounter.set(0)
        while (retryCounter.incrementAndGet() <= maxRetries) {
            try {
                logger.info("Connecting to database, attempt number: {}", retryCounter.get())
                val dataSource = HikariDataSource(config)
                return dataSource
            } catch (th: Throwable) {
                // A missing database will not appear by waiting — let connect() create it.
                if (th.sqlStates().contains(INVALID_CATALOG_NAME)) throw DatabaseMissingException(th)
                logger.info("Connection to database failed: {}", th.message)
                sleep(sleepMs)
            }
        }
        throw IllegalStateException("Connection to database failed")
    }
}

// PostgreSQL SQLSTATE for "database does not exist".
private const val INVALID_CATALOG_NAME = "3D000"

private class DatabaseMissingException(
    cause: Throwable,
) : RuntimeException(cause)

private fun Throwable.sqlStates(): List<String> =
    generateSequence(this) { it.cause }.filterIsInstance<SQLException>().mapNotNull { it.sqlState }.toList()
