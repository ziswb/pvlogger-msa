package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.dataaccessor.rest.readLatestEnergyRegisterValues
import ch.bziswiler.pvlogger.dataaccessor.rest.readSoFarTodayEnergyRegisterValues
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing { readLatestEnergyRegisterValues() }
    routing { readSoFarTodayEnergyRegisterValues() }
}
