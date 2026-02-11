package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.DEVICE_ID
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.JOB_ID
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.REGISTER_ADDRESS
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.TYPE
import io.ktor.server.routing.RoutingContext
import io.github.oshai.kotlinlogging.KotlinLogging

object ParameterRetriever {

    private val logger = KotlinLogging.logger {}

    fun RoutingContext.retrieveTypeParameter(): Type? = when (val value = call.parameters[TYPE]) {
        null -> null
        else -> Type.from(value)
    }

    fun RoutingContext.retrieveRegisterAddressParameter() = try {
        when (val value = call.parameters[REGISTER_ADDRESS]?.toInt()) {
            null -> null
            else -> RegisterAddress(value)
        }
    } catch (e: NumberFormatException) {
        logger.error(e) { "Register address is not convertible to integer" }
        null
    }

    fun RoutingContext.retrieveDeviceIdParameter() = try {
        call.parameters[DEVICE_ID]?.toInt()
    } catch (e: NumberFormatException) {
        logger.error(e) { "Device ID is not convertible to integer" }
        null
    }

    fun RoutingContext.retrieveJobIdParameter(): JobId? = when (val value = call.parameters[JOB_ID]) {
        null -> null
        else -> JobId(value)
    }
}