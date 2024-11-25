package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.logic_device_id
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.response_time
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ShortValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.StringValuesTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UIntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UShortValueTable
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.rest.MasterDataQueryParameterNames.DEVICE_ID
import ch.bziswiler.pvlogger.shared.rest.MasterDataQueryParameterNames.REGISTER_ADDRESS
import ch.bziswiler.pvlogger.shared.rest.MasterDataQueryParameterNames.TYPE
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

private const val ONE = 1

private val logger = KotlinLogging.logger {}
private val responseTimeDescending = response_time to DESC

fun Routing.readRegister() {
    get("/read-latest-register-value/{type}/{deviceId}/{registerAddress}") {
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

private fun PipelineContext<Unit, ApplicationCall>.retrieveTypeParameter(): Type? =
    when (val value = call.parameters[TYPE]) {
        null -> null
        else -> Type.from(value)
    }

private fun PipelineContext<Unit, ApplicationCall>.retrieveRegisterAddressParameter() = try {
    when (val value = call.parameters[REGISTER_ADDRESS]?.toInt()) {
        null -> null
        else -> RegisterAddress(value)
    }
} catch (e: NumberFormatException) {
    logger.error(e) { "Register address is not convertible to integer" }
    null
}

private fun PipelineContext<Unit, ApplicationCall>.retrieveDeviceIdParameter() = try {
    call.parameters[DEVICE_ID]?.toInt()
} catch (e: NumberFormatException) {
    null
}

private fun readLatestRegisterValue(
    type: Type,
    deviceId: Int,
    registerAddress: RegisterAddress
): Any? = when (type) {
    Type.U16, Type.BITFIELD_16 -> readLatestUnsignedShortIntegerRegisterValue(deviceId, registerAddress)
    Type.U32, Type.BITFIELD_32 -> readLatestUnsignedIntegerRegisterValue(deviceId, registerAddress)
    Type.I16 -> readLatestShortIntegerRegisterValue(deviceId, registerAddress)
    Type.I32 -> readLatestIntegerRegisterValue(deviceId, registerAddress)
    Type.STR -> readLatestStringRegisterValue(deviceId, registerAddress)
    Type.MLD -> throw NotImplementedError()
}

private fun readLatestIntegerRegisterValue(
    deviceId: Int,
    registerAddress: RegisterAddress
): Int? =
    transaction {
        (IntegerValueTable innerJoin ResponseTable)
            .select(IntegerValueTable.register_value)
            .where(IntegerValueTable.register_address.eq(registerAddress.value).and(logic_device_id.eq(deviceId.toUByte())))
            .orderBy(responseTimeDescending)
            .limit(ONE)
            .firstOrNull()
            ?.let { row -> row[IntegerValueTable.register_value] }
            ?: null.also { logger.info { "No register value found for device ID '$deviceId' and register address '$registerAddress'." } }

    }

private fun readLatestUnsignedIntegerRegisterValue(
    deviceId: Int,
    registerAddress: RegisterAddress
): UInt? =
    transaction {
        (IntegerValueTable innerJoin ResponseTable)
            .select(UIntegerValueTable.register_value)
            .where(UIntegerValueTable.register_address.eq(registerAddress.value).and(logic_device_id.eq(deviceId.toUByte())))
            .orderBy(responseTimeDescending)
            .limit(ONE)
            .firstOrNull()
            ?.let { row -> row[UIntegerValueTable.register_value]?.toUInt() }
            ?: null.also { logger.info { "No register value found for device ID '$deviceId' and register address '$registerAddress'." } }

    }

private fun readLatestShortIntegerRegisterValue(
    deviceId: Int,
    registerAddress: RegisterAddress
): Short? =
    transaction {
        (ShortValueTable innerJoin ResponseTable)
            .select(ShortValueTable.register_value)
            .where(ShortValueTable.register_address.eq(registerAddress.value).and(logic_device_id.eq(deviceId.toUByte())))
            .orderBy(responseTimeDescending)
            .limit(ONE)
            .firstOrNull()
            ?.let { row -> row[ShortValueTable.register_value] }
            ?: null.also { logger.info { "No register value found for device ID '$deviceId' and register address '$registerAddress'." } }

    }

private fun readLatestUnsignedShortIntegerRegisterValue(
    deviceId: Int,
    registerAddress: RegisterAddress
): UShort? =
    transaction {
        (UShortValueTable innerJoin ResponseTable)
            .select(UShortValueTable.register_value)
            .where(UShortValueTable.register_address.eq(registerAddress.value).and(logic_device_id.eq(deviceId.toUByte())))
            .orderBy(responseTimeDescending)
            .limit(ONE)
            .firstOrNull()
            ?.let { row -> row[UShortValueTable.register_value]?.toUShort() }
            ?: null.also { logger.info { "No register value found for device ID '$deviceId' and register address '$registerAddress'." } }

    }

private fun readLatestStringRegisterValue(
    deviceId: Int,
    registerAddress: RegisterAddress
): String? =
    transaction {
        (StringValuesTable innerJoin ResponseTable)
            .select(StringValuesTable.register_value)
            .where(StringValuesTable.register_address.eq(registerAddress.value).and(logic_device_id.eq(deviceId.toUByte())))
            .orderBy(responseTimeDescending)
            .limit(ONE)
            .firstOrNull()
            ?.let { row -> row[StringValuesTable.register_value] }
            ?: null.also { logger.info { "No register value found for device ID '$deviceId' and register address '$registerAddress'." } }

    }
