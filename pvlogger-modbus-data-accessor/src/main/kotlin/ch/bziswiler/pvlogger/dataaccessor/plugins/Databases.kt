package ch.bziswiler.pvlogger.dataaccessor.plugins

import ch.bziswiler.pvlogger.dataaccessor.database.connection.DatabaseFactory
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    val databaseFactory by inject<DatabaseFactory>()
    databaseFactory.connect()
}
