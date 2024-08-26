package ch.bziswiler.pvlogger.heartbeat.plugins

import ch.bziswiler.pvlogger.heartbeat.database.DatabaseFactory
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    val databaseFactory by inject<DatabaseFactory>()
    databaseFactory.connect()
}
