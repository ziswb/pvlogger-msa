package ch.bziswiler.pvlogger.masterdata.server.plugins

import ch.bziswiler.pvlogger.masterdata.server.database.connection.DatabaseFactory
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    val databaseFactory by inject<DatabaseFactory>()
    databaseFactory.connect()
}
