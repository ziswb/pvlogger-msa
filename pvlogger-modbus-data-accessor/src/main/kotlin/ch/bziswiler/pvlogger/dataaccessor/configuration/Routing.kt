package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.dataaccessor.rest.readHouseKpiValues
import ch.bziswiler.pvlogger.dataaccessor.rest.readKwSoFarToday
import ch.bziswiler.pvlogger.dataaccessor.rest.readLatestEnergyRegisterValues
import ch.bziswiler.pvlogger.dataaccessor.rest.storeKpiValues
import ch.bziswiler.pvlogger.dataaccessor.rest.storeModbusValues
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing { readLatestEnergyRegisterValues() }
    routing { readKwSoFarToday() }
    routing { readHouseKpiValues() }
    routing { storeModbusValues() }
    routing { storeKpiValues() }
}
