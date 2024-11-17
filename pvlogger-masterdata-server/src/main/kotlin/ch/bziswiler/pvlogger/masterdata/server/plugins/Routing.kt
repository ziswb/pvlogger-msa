package ch.bziswiler.pvlogger.masterdata.server.plugins

import ch.bziswiler.pvlogger.masterdata.server.service.importMasterData
import ch.bziswiler.pvlogger.masterdata.server.service.masterDataForAddress
import ch.bziswiler.pvlogger.masterdata.server.service.masterDataForCategoriesAndDeviceTypes
import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

const val ROOT_PATH = "/"
const val IMPORT_MASTER_DATA_PATH = "/import-master-data"
const val MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES = "/master-data"
const val MASTER_DATA_FOR_ADDRESS_PATH = "/master-data/{address}"

fun Application.configureRouting() {
    routing {
        put(IMPORT_MASTER_DATA_PATH) { importMasterData() }
        get(MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES) { masterDataForCategoriesAndDeviceTypes() }
        get(MASTER_DATA_FOR_ADDRESS_PATH) { masterDataForAddress() }
    }
}
