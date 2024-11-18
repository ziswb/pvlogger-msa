package ch.bziswiler.pvlogger.masterdata.server.configuration

import ch.bziswiler.pvlogger.masterdata.server.rest.importMasterData
import ch.bziswiler.pvlogger.masterdata.server.rest.masterDataForAddress
import ch.bziswiler.pvlogger.masterdata.server.rest.masterDataForCategoriesAndDeviceTypes
import ch.bziswiler.pvlogger.shared.rest.MasterDataPathConstants.IMPORT_MASTER_DATA_PATH
import ch.bziswiler.pvlogger.shared.rest.MasterDataPathConstants.MASTER_DATA_FOR_ADDRESS_PATH
import ch.bziswiler.pvlogger.shared.rest.MasterDataPathConstants.MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES
import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

private const val MASTER_DATA_FOR_ADDRESS_PATH_WITH_PARAM = "${MASTER_DATA_FOR_ADDRESS_PATH}/{address}"

fun Application.configureRouting() {
    routing {
        put(IMPORT_MASTER_DATA_PATH) { importMasterData() }
        get(MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES) { masterDataForCategoriesAndDeviceTypes() }
        get(MASTER_DATA_FOR_ADDRESS_PATH_WITH_PARAM) { masterDataForAddress() }
    }
}
