package ch.bziswiler.pvlogger.datacollector.services.modbus

import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.ModbusRegisterDefinition.Companion.CHAR_SET
import kotlinx.io.Source
import kotlinx.io.readString
import java.nio.charset.Charset.forName

fun Source.readString(nOfBytes: Int): String = readString(nOfBytes.toLong(), forName(CHAR_SET))

fun Source.readMld(nOfShorts: Int): ShortArray {
    var result = shortArrayOf()
    repeat(nOfShorts) { result = result.plus(this.readShort()) }
    return result
}

fun Source.readUInt(): UInt = this.readInt().toUInt()

fun Source.readUShort(): UShort = this.readShort().toUShort()

fun Source.readUByte(): UByte = this.readByte().toUByte()
