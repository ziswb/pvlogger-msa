package ch.bziswiler.pvlogger.heartbeat.database

import ch.bziswiler.pvlogger.heartbeat.configuration.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway.configure
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Database.Companion.connect
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class DatabaseFactoryImpl(appConfig: AppConfig) : DatabaseFactory {

    private val dbConfig = appConfig.databaseConfig

    override fun connect() {
        assertDatabaseExists(connect(hikari(dbConfig.dbServerUrl)))
        val dataSource = hikari(dbConfig.dbServerCanonicalUrl)
        connect(dataSource)
        migrateDatabase(dataSource)
    }

    override fun close() {
        // not needed
    }

    private fun assertDatabaseExists(database: Database) {
        if (databaseDoesNotExist(database)) {
            createDatabase(database)
        }
    }

    private fun databaseDoesNotExist(database: Database): Boolean =
        transaction(database) {
            SchemaUtils.listDatabases().find { it == dbConfig.dbName }.isNullOrEmpty()
        }

    private fun createDatabase(database: Database) {
        transaction(database) {
            connection.autoCommit = true
            SchemaUtils.createDatabase(databases = arrayOf(dbConfig.dbName))
            connection.autoCommit = false
        }
    }

    private fun migrateDatabase(dataSource: DataSource) {
        configure().dataSource(dataSource).load().migrate()
    }

    private fun hikari(dbServerUrl: String): DataSource {
        val config =
            HikariConfig().apply {
                driverClassName = dbConfig.driverClass
                jdbcUrl = dbServerUrl
                username = dbConfig.user
                password = dbConfig.password
                maximumPoolSize = dbConfig.maxPoolSize
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }
        return HikariDataSource(config)
    }
}
