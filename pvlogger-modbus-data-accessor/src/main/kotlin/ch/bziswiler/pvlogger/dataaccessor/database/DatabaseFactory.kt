package ch.bziswiler.pvlogger.dataaccessor.database

import ch.bziswiler.pvlogger.dataaccessor.config.AppConfig
import ch.bziswiler.pvlogger.dataaccessor.schema.CharacteristicCurveValuesTable
import ch.bziswiler.pvlogger.dataaccessor.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.schema.ShortValueTable
import ch.bziswiler.pvlogger.dataaccessor.schema.StringValuesTable
import ch.bziswiler.pvlogger.dataaccessor.schema.UIntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.schema.UShortValueTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.listDatabases
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class DatabaseFactory(appConfig: AppConfig) {

    private val dbConfig = appConfig.databaseConfig

    fun connect() {
        assertDatabaseExists(Database.connect(hikari(dbConfig.dbServerUrl)))
        assertSchemaComplete(Database.connect(hikari(dbConfig.dbServerCanonicalUrl)))
    }

    private fun assertSchemaComplete(database: Database) {
        transaction(database) {
            create(
                CharacteristicCurveValuesTable,
                IntegerValueTable,
                ResponseTable,
                ShortValueTable,
                StringValuesTable,
                UIntegerValueTable,
                UShortValueTable
            )
        }
    }

    private fun assertDatabaseExists(database: Database) {
        transaction(database) {
            if (listDatabases().find { it == dbConfig.dbName }.isNullOrEmpty()) {
                connection.autoCommit = true
                SchemaUtils.createDatabase(databases = arrayOf(dbConfig.dbName), inBatch = true)
                connection.autoCommit = false
            }
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
