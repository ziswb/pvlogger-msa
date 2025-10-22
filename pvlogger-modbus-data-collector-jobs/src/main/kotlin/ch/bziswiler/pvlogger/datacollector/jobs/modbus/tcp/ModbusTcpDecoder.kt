package ch.bziswiler.pvlogger.datacollector.jobs.modbus.tcp

import ch.bziswiler.pvlogger.shared.modbus.ModbusObject
import okio.BufferedSource

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun decodeModbusObject(source: BufferedSource): ModbusObject {
    val objectId = source.readUByte()
    val objectLength = source.readUByte()
    var objectValue = ubyteArrayOf()
    repeat(objectLength.toInt()) { objectValue = objectValue.plus(source.readUByte()) }
    return ModbusObject(objectId, objectLength, objectValue)
}
