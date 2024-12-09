package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveDeviceIdParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveJobIdParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveRegisterAddressParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveTypeParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.RegisterReader.readLatestRegisterValue
import ch.bziswiler.pvlogger.dataaccessor.rest.RegisterReader.readSoFarTodayRegisterValue
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.DEVICE_ID
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.JOB_ID
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.REGISTER_ADDRESS
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.TYPE
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_TODAY_SO_FAR_REGISTER_VALUE_PATH
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.readLatestEnergyRegisterValues() {
    get("/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH/{$TYPE}/{$DEVICE_ID}/{$REGISTER_ADDRESS}/{$JOB_ID}") {
        val type = retrieveTypeParameter()
        val deviceId = retrieveDeviceIdParameter()
        val registerAddress = retrieveRegisterAddressParameter()
        val jobId = retrieveJobIdParameter()
        if (type == null) {
            call.respond(BadRequest, "type is required")
        } else if (deviceId == null) {
            call.respond(BadRequest, "deviceId is required")
        } else if (registerAddress == null) {
            call.respond(BadRequest, "address is required")
        } else if (jobId == null) {
            call.respond(BadRequest, "jobId is required")
        } else {
            when (val value = readLatestRegisterValue(type, deviceId, registerAddress, jobId)) {
                null -> call.respond(NotFound)
                else -> call.respond(OK, value)
            }
        }
    }
}

fun Routing.readSoFarTodayEnergyRegisterValues() {
    get("/$DATA_ACCESSOR_READ_TODAY_SO_FAR_REGISTER_VALUE_PATH/{$TYPE}/{$REGISTER_ADDRESS}") {
        val type = retrieveTypeParameter()
        val registerAddress = retrieveRegisterAddressParameter()
        if (type == null) {
            call.respond(BadRequest, "type is required")
        } else if (registerAddress == null) {
            call.respond(BadRequest, "address is required")
        } else {
            readSoFarTodayRegisterValue(type, registerAddress).let {
                when (it) {
                    null -> call.respond(NotFound)
                    else -> call.respond(OK, it)
                }
            }
            readSoFarTodayRegisterValue(type, registerAddress).let {
                when (it) {
                    null -> call.respond(NotFound)
                    else -> call.respond(OK, it)
                }
            }
        }
    }
}
