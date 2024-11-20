package ch.bziswiler.pvlogger.masterdata.server.configuration

import ch.bziswiler.pvlogger.masterdata.server.rest.importMasterData
import ch.bziswiler.pvlogger.masterdata.server.rest.masterDataForAddress
import ch.bziswiler.pvlogger.masterdata.server.rest.masterDataForCategoriesAndDeviceTypes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        importMasterData()
        masterDataForCategoriesAndDeviceTypes()
        masterDataForAddress()
    }
}
