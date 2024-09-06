package ch.bziswiler.pvlogger.datacollector.modbus.tcp

import ch.bziswiler.pvlogger.shared.modbus.ModbusObject
import io.ktor.utils.io.ByteReadChannel

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun decodeModbusObject(channel: ByteReadChannel): ModbusObject {
    val objectId = channel.readByte().toUByte()
    val objectLength = channel.readByte().toUByte()
    var objectValue = ubyteArrayOf()
    repeat(objectLength.toInt()) { objectValue = objectValue.plus(channel.readUByte()) }
    return ModbusObject(objectId, objectLength, objectValue)
}
