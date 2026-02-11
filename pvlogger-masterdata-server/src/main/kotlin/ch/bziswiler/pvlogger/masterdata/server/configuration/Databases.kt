package ch.bziswiler.pvlogger.masterdata.server.configuration

import ch.bziswiler.pvlogger.shared.database.DatabaseFactory
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    val databaseFactory by inject<DatabaseFactory>()
    databaseFactory.connect()
}
