package ch.bziswiler.pvlogger.masterdata.server.database.connection

import ch.bziswiler.pvlogger.masterdata.server.configuration.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.database.schema.MasterDataTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class DatabaseFactory(appConfig: AppConfig) {

    private val dbConfig = appConfig.databaseConfig

    fun connect() {
        assertDatabaseExists(Database.connect(hikari(dbConfig.dbServerUrl)))
        assertSchemaComplete(Database.connect(hikari(dbConfig.dbServerCanonicalUrl)))
    }

    private fun assertSchemaComplete(database: Database) = transaction(database) { SchemaUtils.create(MasterDataTable) }

    private fun assertDatabaseExists(database: Database) = transaction(database) {
        if (SchemaUtils.listDatabases().find { it == dbConfig.dbName }.isNullOrEmpty()) {
            connection.autoCommit = true
            SchemaUtils.createDatabase(databases = arrayOf(dbConfig.dbName), inBatch = true)
            connection.autoCommit = false
        }
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
