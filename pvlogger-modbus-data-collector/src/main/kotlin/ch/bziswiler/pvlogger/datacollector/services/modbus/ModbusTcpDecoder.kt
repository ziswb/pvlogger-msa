package ch.bziswiler.pvlogger.datacollector.services.modbus

import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusObject
import kotlinx.io.Source

@OptIn(ExperimentalUnsignedTypes::class)
fun decodeModbusObject(source: Source): ModbusObject {
    val objectId = source.readUByte()
    val objectLength = source.readUByte()
    var objectValue = ubyteArrayOf()
    repeat(objectLength.toInt()) { objectValue = objectValue.plus(source.readUByte()) }
    return ModbusObject(objectId, objectLength, objectValue)
}
