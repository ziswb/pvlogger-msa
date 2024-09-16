package ch.bziswiler.pvlogger.masterdata.server.plugins

import ch.bziswiler.pvlogger.masterdata.server.config.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.modbus.schema.MasterDataDao
import ch.bziswiler.pvlogger.masterdata.server.modbus.services.MasterDataImporter
import ch.bziswiler.pvlogger.masterdata.server.modbus.services.MasterDataService
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.shared.masterdata.request.MasterDataRequesCategory
import ch.bziswiler.pvlogger.shared.masterdata.request.MasterDataRequesCategory.CATEGORIES
import ch.bziswiler.pvlogger.shared.masterdata.request.MasterDataRequesCategory.DEVICE_TYPES
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
import org.koin.java.KoinJavaComponent.inject
import org.koin.ktor.ext.inject

const val ROOT_PATH = "/"
const val IMPORT_MASTERDATA_PATH = "/import-masterdata"
const val REGISTER_DEFINITIONS_PATH = "/register-definitions"
const val GAIN_PATH = "/gain/{address}"
private const val PARAM_VALUE_DELIMITER = ";"

fun Application.configureRouting() {
    val logger = KotlinLogging.logger {}
    routing {
        val masterDataService by inject<MasterDataService>()
        put(IMPORT_MASTERDATA_PATH) {
            val appConfig by inject<AppConfig>(AppConfig::class.java)
            val dao by inject<MasterDataDao>(MasterDataDao::class.java)
            val importer = MasterDataImporter(appConfig, dao)
            importer.importInverterRegister()
            importer.importDeviceIdentifiers()
            importer.importMeterRegister()
            call.response.status(HttpStatusCode.OK)
        }
        get(REGISTER_DEFINITIONS_PATH) {
            try {
                val categories = getParameterValues(CATEGORIES)?.map { Category.valueOf(it) }?.toList() ?: emptyList()
                val deviceTypes = getParameterValues(DEVICE_TYPES)?.map { DeviceType.valueOf(it) }?.toList() ?: emptyList()
                val result = masterDataService.registerDefinitionList(categories, deviceTypes)
                call.response.status(HttpStatusCode.OK)
                call.respond(result)
            } catch (t: Throwable) {
                logger.error { t }
                call.response.status(HttpStatusCode.BadRequest)
                call.respond(t)
            }
        }
        get(GAIN_PATH) {
            try {
                when (val address = call.parameters["address"]?.toInt()) {
                    null -> {
                        call.response.status(HttpStatusCode.BadRequest)
                        call.respond("No parameter 'address' of type Int given.")
                    }

                    else -> {
                        logger.info { "address: $address" }
                        val result: String? = masterDataService.gainForRegisterAddress(address)
                        logger.info { "result: $result" }
                        if (result != null) {
                            call.response.status(HttpStatusCode.OK)
                            call.respondText(result, ContentType.Text.Plain)
                        } else {
                            call.response.status(HttpStatusCode.BadRequest)
                            call.respond("No gain for register address '$address' found")
                        }
                    }
                }
            } catch (t: Throwable) {
                logger.error { t }
                call.response.status(HttpStatusCode.BadRequest)
                call.respond(t)
            }
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.getParameterValues(category: MasterDataRequesCategory) = call.request.queryParameters[category.value]?.split(PARAM_VALUE_DELIMITER)
