package ch.bziswiler.pvlogger.shared.databbase

import ch.bziswiler.pvlogger.shared.configuration.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.flywaydb.core.Flyway.configure
import org.jetbrains.exposed.sql.Database.Companion.connect
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger
import javax.jms.JMSException
import javax.sql.DataSource

class DatabaseFactory(private val dbConfig: DatabaseConfig) {

    private val logger = KotlinLogging.logger {}
    private val maxRetries = 20
    private val sleepMs: Long = 5000
    private val retryCounter = AtomicInteger(0)

    fun connect() {
        connect(hikari(dbConfig.dbServerUrl))
        assertDatabaseExists()
        connect(hikari(dbConfig.dbServerCanonicalUrl))
        migrateDatabase()
    }

    private fun assertDatabaseExists() {
        if (databaseDoesNotExist()) {
            createDatabase()
        }
    }

    private fun databaseDoesNotExist(): Boolean = transaction {
        SchemaUtils.listDatabases().find { it == dbConfig.dbName }.isNullOrEmpty()
    }

    private fun createDatabase() {
        transaction {
            connection.autoCommit = true
            SchemaUtils.createDatabase(databases = arrayOf(dbConfig.dbName))
            connection.autoCommit = false
        }
    }

    private fun migrateDatabase() {
        configure().dataSource(dbConfig.dbServerCanonicalUrl, dbConfig.user, dbConfig.password).load().migrate()
    }

    private fun hikari(dbServerUrl: String): DataSource {
        val config = HikariConfig().apply {
            driverClassName = dbConfig.driverClass
            jdbcUrl = dbServerUrl
            username = dbConfig.user
            password = dbConfig.password
            maximumPoolSize = dbConfig.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return tryConnectingToDatabase(config)
    }

    private fun tryConnectingToDatabase(config: HikariConfig): HikariDataSource {
        retryCounter.set(0)
        while (retryCounter.incrementAndGet() <= maxRetries) {
            try {
                logger.info("Connecting to database, attempt number: {}", retryCounter.get())
                return HikariDataSource(config)
            } catch (jmse: JMSException) {
                logger.info("Connection to database failed: {}", jmse.message)
                sleep(sleepMs)
            }
        }
        throw IllegalStateException("Connection to database failed")
    }
}
