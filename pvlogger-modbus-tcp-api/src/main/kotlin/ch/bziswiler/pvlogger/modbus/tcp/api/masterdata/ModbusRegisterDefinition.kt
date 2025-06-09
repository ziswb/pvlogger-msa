package ch.bziswiler.pvlogger.modbus.tcp.api.masterdata

import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.ModbusRegisterDefinition.Companion.CHAR_SET
import java.nio.charset.Charset

data class ModbusRegisterDefinition(
    val no: Int,
    val name: String,
    val readWrite: ReadWrite,
    val type: Type,
    val unit: Unit,
    val gain: Gain,
    val registerAddress: RegisterAddress,
    val quantity: Quantity,
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
