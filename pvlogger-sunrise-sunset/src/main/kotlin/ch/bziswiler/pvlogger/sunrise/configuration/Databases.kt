package ch.bziswiler.pvlogger.sunrise.configuration

import ch.bziswiler.pvlogger.shared.database.DatabaseFactory
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

fun Application.configureDatabase() {
    val databaseFactory by inject<DatabaseFactory>()
    databaseFactory.connect()
}
