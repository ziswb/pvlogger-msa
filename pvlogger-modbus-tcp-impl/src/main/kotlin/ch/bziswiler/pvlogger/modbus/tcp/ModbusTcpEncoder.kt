package ch.bziswiler.pvlogger.modbus.tcp

import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.modbus.tcp.frame.UByteData
import ch.bziswiler.pvlogger.modbus.tcp.frame.UShortData
import okio.BufferedSink

@OptIn(ExperimentalUnsignedTypes::class)
fun MbapHeader.encode(sink: BufferedSink) {
    sink.writeUShort(protocolIdentifier)
    sink.writeUShort(protocolType)
    sink.writeUShort(dataLength)
    sink.writeUByte(logicDeviceId)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun UByteData.encode(sink: BufferedSink) {
    data.forEach { sink.writeUByte(it) }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun UShortData.encode(sink: BufferedSink) {
    data.forEach { sink.writeUShort(it) }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun FunctionCode.encode(sink: BufferedSink) {
    sink.writeUByte(value)
}
