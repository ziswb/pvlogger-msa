package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.DEVICE_ID
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.REGISTER_ADDRESS
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.TYPE
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging

object ParameterRetriever {

    private val logger = KotlinLogging.logger {}

    fun PipelineContext<Unit, ApplicationCall>.retrieveTypeParameter(): Type? = when (val value = call.parameters[TYPE]) {
        null -> null
        else -> Type.from(value)
    }

    fun PipelineContext<Unit, ApplicationCall>.retrieveRegisterAddressParameter() = try {
        when (val value = call.parameters[REGISTER_ADDRESS]?.toInt()) {
            null -> null
            else -> RegisterAddress(value)
        }
    } catch (e: NumberFormatException) {
        logger.error(e) { "Register address is not convertible to integer" }
        null
    }

    fun PipelineContext<Unit, ApplicationCall>.retrieveDeviceIdParameter() = try {
        call.parameters[DEVICE_ID]?.toInt()
    } catch (e: NumberFormatException) {
        logger.error(e) { "Device ID is not convertible to integer" }
        null
    }

}