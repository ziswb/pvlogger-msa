package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.shared.database.DatabaseFactory
import ch.bziswiler.pvlogger.shared.ktor.ReadinessCheck
import ch.bziswiler.pvlogger.shared.ktor.closeOnShutdown
import ch.bziswiler.pvlogger.shared.ktor.configureHealth
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    val databaseFactory by inject<DatabaseFactory>()
    databaseFactory.connect()
    closeOnShutdown(databaseFactory)
    // Ready while a pooled connection answers (backlog 60).
    configureHealth(mapOf("database" to ReadinessCheck { databaseFactory.checkReachable() }))
}
