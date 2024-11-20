package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable.register_address
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable.register_value
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.logic_device_id
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.response_time
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Gain
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.rest.MasterDataQueryParameterNames.DEVICE_ID
import ch.bziswiler.pvlogger.shared.rest.MasterDataQueryParameterNames.GAIN
import ch.bziswiler.pvlogger.shared.rest.MasterDataQueryParameterNames.REGISTER_ADDRESS
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

object ReadRegisterValueController {

    private const val ONE = 1

    private val logger = KotlinLogging.logger {}
    private val responseTimeDescending = response_time to DESC

    fun Routing.readRegister() {
        get("/read-latest-register-value/{deviceId}/{registerAddress}") {
            val deviceId = retrieveDeviceIdParameter()
            val registerAddress = retrieveRegisterAddressParameter()
            val gain = retrieveGainQueryParameter()
            if (deviceId == null) {
                call.respond(HttpStatusCode.BadRequest, "deviceId is required")
            } else if (registerAddress == null) {
                call.respond(HttpStatusCode.BadRequest, "address is required")
            } else if (gain == null) {
                call.respond(HttpStatusCode.BadRequest, "gain is required")
            } else {
                when (val value = readAndTransposeLatestRegisterValue(deviceId, registerAddress, gain)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(HttpStatusCode.OK, value)
                }
            }
        }
    }

    private fun PipelineContext<Unit, ApplicationCall>.retrieveGainQueryParameter() = try {
        when (val value = call.request.queryParameters[GAIN]) {
            null -> null
            else -> Gain.from(value)
        }
    } catch (e: Exception) {
        null
    }

    private fun PipelineContext<Unit, ApplicationCall>.retrieveRegisterAddressParameter() = try {
        when (val value = call.parameters[REGISTER_ADDRESS]?.toInt()) {
            null -> null
            else -> RegisterAddress(value)
        }
    } catch (e: NumberFormatException) {
        null
    }

    private fun PipelineContext<Unit, ApplicationCall>.retrieveDeviceIdParameter() = try {
        call.parameters[DEVICE_ID]?.toInt()
    } catch (e: NumberFormatException) {
        null
    }

    private fun readAndTransposeLatestRegisterValue(
        deviceId: Int,
        registerAddress: RegisterAddress,
        gain: Gain
    ): Double? =
        transaction {
            (IntegerValueTable innerJoin ResponseTable)
                .select(register_value)
                .where(register_address.eq(registerAddress.value).and(logic_device_id.eq(deviceId.toUByte())))
                .orderBy(responseTimeDescending)
                .limit(ONE)
                .firstOrNull()
                ?.let { row -> row[register_value]?.div(gain.raw.toDouble()) }
                ?: null.also { logger.info { "No register value found for device ID '$deviceId' and register address '$registerAddress'." } }
        }
}
