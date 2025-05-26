package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ShortValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.StringValuesTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UIntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UShortValueTable
import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveDeviceIdParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveJobIdParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveRegisterAddressParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.ParameterRetriever.retrieveTypeParameter
import ch.bziswiler.pvlogger.dataaccessor.rest.RegisterReader.readKwSoFarToday
import ch.bziswiler.pvlogger.dataaccessor.rest.RegisterReader.readLatestRegisterValue
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.DEVICE_ID
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.JOB_ID
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.REGISTER_ADDRESS
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.TYPE
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_KW_TODAY_SO_FAR_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.Measurement
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.util.pipeline.PipelineContext

fun Routing.readLatestEnergyRegisterValues() {
    suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest(type: Type, deviceId: Int, registerAddress: RegisterAddress, jobId: JobId) {

        suspend fun handleMessage(message: Measurement<UShort>?) {
            when (message) {
                null -> call.respond(NotFound)
                else -> call.respond(OK, message)
            }
        }

        suspend fun handleMessage(message: Measurement<UInt>?) {
            when (message) {
                null -> call.respond(NotFound)
                else -> call.respond(OK, message)
            }
        }

        suspend fun handleMessage(message: Measurement<Short>?) {
            when (message) {
                null -> call.respond(NotFound)
                else -> call.respond(OK, message)
            }
        }

        suspend fun handleMessage(message: Measurement<Int>?) {
            when (message) {
                null -> call.respond(NotFound)
                else -> call.respond(OK, message)
            }
        }

        suspend fun handleMessage(message: Measurement<String>?) {
            when (message) {
                null -> call.respond(NotFound)
                else -> call.respond(OK, message)
            }
        }

        when (type) {
            Type.U16, Type.BITFIELD_16 -> handleMessage(readLatestRegisterValue(UShortValueTable, UShortValueTable.register_value, UShortValueTable.register_address, registerAddress, deviceId, jobId))
            Type.U32, Type.BITFIELD_32 -> handleMessage(readLatestRegisterValue(UIntegerValueTable, UIntegerValueTable.register_value, UIntegerValueTable.register_address, registerAddress, deviceId, jobId))
            Type.I16 -> handleMessage(readLatestRegisterValue(ShortValueTable, ShortValueTable.register_value, ShortValueTable.register_address, registerAddress, deviceId, jobId))
            Type.I32 -> handleMessage(readLatestRegisterValue(IntegerValueTable, IntegerValueTable.register_value, IntegerValueTable.register_address, registerAddress, deviceId, jobId))
            Type.STR -> handleMessage(readLatestRegisterValue(StringValuesTable, StringValuesTable.register_value, StringValuesTable.register_address, registerAddress, deviceId, jobId))
            Type.MLD -> throw NotImplementedError()
        }
    }

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
            handleRequest(type, deviceId, registerAddress, jobId)
        }
    }
}

fun Routing.readKwSoFarToday() {
    get("/$DATA_ACCESSOR_READ_KW_TODAY_SO_FAR_PATH/{$TYPE}/{$REGISTER_ADDRESS}") {
        val type = retrieveTypeParameter()
        val registerAddress = retrieveRegisterAddressParameter()
        if (type == null) {
            call.respond(BadRequest, "type is required")
        } else if (registerAddress == null) {
            call.respond(BadRequest, "address is required")
        } else {
            readKwSoFarToday(type, registerAddress).let {
                when (it) {
                    null -> call.respond(NotFound)
                    else -> call.respond(OK, it)
                }
            }
        }
    }
}
