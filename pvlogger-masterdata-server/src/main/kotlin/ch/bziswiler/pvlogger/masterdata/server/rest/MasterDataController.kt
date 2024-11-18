package ch.bziswiler.pvlogger.masterdata.server.rest

import ch.bziswiler.pvlogger.masterdata.server.configuration.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.database.access.MasterDataDao
import ch.bziswiler.pvlogger.masterdata.server.database.schema.MasterDataTable.category
import ch.bziswiler.pvlogger.masterdata.server.database.schema.MasterDataTable.deviceType
import ch.bziswiler.pvlogger.masterdata.server.service.MasterDataImporter
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.shared.masterdata.request.MasterDataRequestParameters
import ch.bziswiler.pvlogger.shared.masterdata.request.MasterDataRequestParameters.ADDRESS
import ch.bziswiler.pvlogger.shared.masterdata.request.MasterDataRequestParameters.CATEGORIES
import ch.bziswiler.pvlogger.shared.masterdata.request.MasterDataRequestParameters.DEVICE_TYPES
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.koin.java.KoinJavaComponent.inject

private const val PARAM_VALUE_DELIMITER = ";"

private val dao: MasterDataDao by inject(MasterDataDao::class.java)
private val logger = KotlinLogging.logger {}

suspend fun PipelineContext<Unit, ApplicationCall>.masterDataForAddress() {
    val address = call.parameters[ADDRESS.value]?.toInt() ?: -1
    val result = dao.registerDefinition(address)
    if (result == null) {
        call.response.status(HttpStatusCode.NoContent)
        call.respond("No register definition found for address '$address'")
    } else {
        call.response.status(HttpStatusCode.OK)
        call.respond(result)
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.masterDataForCategoriesAndDeviceTypes() {
    runCatching {
        val categories = getParameterValues(CATEGORIES)?.map { Category.valueOf(it) }?.toList() ?: emptyList()
        val deviceTypes = getParameterValues(DEVICE_TYPES)?.map { DeviceType.valueOf(it) }?.toList() ?: emptyList()
        dao.registerDefinitionList(whereCondition(categories, deviceTypes))
    }.onSuccess { result ->
        if (result.isEmpty()) {
            call.response.status(HttpStatusCode.NoContent)
            call.respond("No master data found")
        } else {
            call.response.status(HttpStatusCode.OK)
            call.respond(result)
        }
    }.onFailure { cause ->
        logger.error { cause }
        call.response.status(HttpStatusCode.BadRequest)
        call.respond("Error looking up master data")
    }
}

fun PipelineContext<Unit, ApplicationCall>.importMasterData() {
    val appConfig by inject<AppConfig>(AppConfig::class.java)
    val dao by inject<MasterDataDao>(MasterDataDao::class.java)
    val importer = MasterDataImporter(appConfig, dao)
    importer.importInverterRegister()
    importer.importDeviceIdentifiers()
    importer.importMeterRegister()
    call.response.status(HttpStatusCode.OK)
}

private fun PipelineContext<Unit, ApplicationCall>.getParameterValues(category: MasterDataRequestParameters) =
    call.request.queryParameters[category.value]?.split(PARAM_VALUE_DELIMITER)

private fun whereCondition(categories: List<Category>, deviceTypes: List<DeviceType>) =
    category.inList(categories).and(deviceType.inList(deviceTypes))
