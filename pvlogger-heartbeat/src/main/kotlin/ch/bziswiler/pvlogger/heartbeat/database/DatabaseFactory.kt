package ch.bziswiler.pvlogger.heartbeat.database

import ch.bziswiler.pvlogger.heartbeat.configuration.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway.configure
import org.jetbrains.exposed.sql.Database.Companion.connect
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class DatabaseFactory(appConfig: AppConfig) {

    private val dbConfig = appConfig.databaseConfig

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
        return HikariDataSource(config)
    }
}
