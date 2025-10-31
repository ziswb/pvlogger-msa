package ch.bziswiler.pvlogger.modbus.tcp

import ch.bziswiler.pvlogger.modbus.api.tcp.readUByte
import okio.BufferedSource

@OptIn(ExperimentalUnsignedTypes::class)
fun decodeModbusObject(source: BufferedSource): ModbusObject {
    val objectId = source.readUByte()
    val objectLength = source.readUByte()
    var objectValue = ubyteArrayOf()
    repeat(objectLength.toInt()) { objectValue = objectValue.plus(source.readUByte()) }
    return ModbusObject(objectId, objectLength, objectValue)
}
