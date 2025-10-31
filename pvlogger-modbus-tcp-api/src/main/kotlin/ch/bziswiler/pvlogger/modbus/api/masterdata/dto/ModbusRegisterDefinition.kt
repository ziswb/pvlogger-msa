package ch.bziswiler.pvlogger.modbus.api.masterdata.dto

import ch.bziswiler.pvlogger.modbus.api.masterdata.Category
import ch.bziswiler.pvlogger.modbus.api.masterdata.Gain
import ch.bziswiler.pvlogger.modbus.api.masterdata.Quantity
import ch.bziswiler.pvlogger.modbus.api.masterdata.ReadWrite
import ch.bziswiler.pvlogger.modbus.api.masterdata.RegisterAddress
import ch.bziswiler.pvlogger.modbus.api.masterdata.Scope
import ch.bziswiler.pvlogger.modbus.api.masterdata.Type
import ch.bziswiler.pvlogger.modbus.api.masterdata.Unit
import ch.bziswiler.pvlogger.modbus.api.masterdata.dto.ModbusRegisterDefinition.Companion.CHAR_SET
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.nio.charset.Charset

@Serializable
data class ModbusRegisterDefinition(
    val no: Int,
    val name: String,
    val readWrite: ReadWrite,
    val type: Type,
    val unit: Unit,
    val gain: Gain,
    @Contextual
    val registerAddress: RegisterAddress,
    @Contextual
    val quantity: Quantity,
    @Contextual
    val scope: Scope,
    val category: Category,
) {
    companion object {
        const val CHAR_SET = "ISO-8859-5"
    }
}

fun ByteArray.stringify() = this
    .toString(Charset.forName(CHAR_SET))
    .replace("\u0000", " ")
    .trim()
