package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveDeviceIdParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveRegisterAddressParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveTypeParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.RegisterReader.readLatestRegisterValue
import ch.bziswiler.pvlogger.dataaccessor.rest.RegisterReader.readSoFarTodayRegisterValue
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_TODAY_SO_FAR_REGISTER_VALUE_PATH
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.readLatestEnergyRegisterValues() {
    get("/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH/{type}/{deviceId}/{registerAddress}") {
        val type = retrieveTypeParameter()
        val deviceId = retrieveDeviceIdParameter()
        val registerAddress = retrieveRegisterAddressParameter()
        if (type == null) {
            call.respond(HttpStatusCode.BadRequest, "type is required")
        } else if (deviceId == null) {
            call.respond(HttpStatusCode.BadRequest, "deviceId is required")
        } else if (registerAddress == null) {
            call.respond(HttpStatusCode.BadRequest, "address is required")
        } else {
            when (val value = readLatestRegisterValue(type, deviceId, registerAddress)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(HttpStatusCode.OK, value)
            }
        }
    }
}

fun Routing.readSoFarTodayEnergyRegisterValues() {
    get("/$DATA_ACCESSOR_READ_TODAY_SO_FAR_REGISTER_VALUE_PATH/{type}/{registerAddress}") {
        val type = retrieveTypeParameter()
        val registerAddress = retrieveRegisterAddressParameter()
        if (type == null) {
            call.respond(HttpStatusCode.BadRequest, "type is required")
        } else if (registerAddress == null) {
            call.respond(HttpStatusCode.BadRequest, "address is required")
        } else {
            readSoFarTodayRegisterValue(type, registerAddress).let { call.respond(HttpStatusCode.OK, it) }
        }
    }
}
