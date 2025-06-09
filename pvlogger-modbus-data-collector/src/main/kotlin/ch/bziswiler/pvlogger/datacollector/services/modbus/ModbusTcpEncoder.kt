package ch.bziswiler.pvlogger.datacollector.services.modbus

import ch.bziswiler.pvlogger.modbus.tcp.api.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.MbapHeader
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.UByteData
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.UShortData
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.writeUByte
import kotlinx.io.writeUShort

/**
The kotlin.UByte is an unsigned 8-bit integer (0 – 255)
The kotlin.UShort is an unsigned 16-bit integer (0 – 65535)
The kotlin.UInt is an unsigned 32-bit integer (0 to 2^32 – 1)
The kotlin.ULong is an unsigned 64-bit integer (0 to 2^64 -1)
 */
fun MbapHeader.encode(sink: Sink) = Buffer().apply {
    writeUShort(protocolIdentifier)
    writeUShort(protocolIdentifier)
    writeUShort(protocolType)
    writeUShort(dataLength)
    writeUByte(logicDeviceId)
    transferTo(sink)
}

fun UByteData.encode(sink: Sink) = Buffer().apply {
    data.forEach { writeUByte(it) }
    transferTo(sink)
}

fun UShortData.encode(sink: Sink) = Buffer().apply {
    data.forEach { writeUShort(it) }
    transferTo(sink)
}

fun FunctionCode.encode(sink: Sink) = Buffer().apply {
    writeUByte(value)
    transferTo(sink)
}
