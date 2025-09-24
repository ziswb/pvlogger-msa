package ch.bziswiler.pvlogger.datacollector.modbus.tcp

import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.UByteData
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.UShortData
import okio.BufferedSink

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun MbapHeader.encode(sink: BufferedSink) {
    sink.writeUShort(protocolIdentifier)
    sink.writeUShort(protocolType)
    sink.writeUShort(dataLength)
    sink.writeUByte(logicDeviceId)
}

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun UByteData.encode(sink: BufferedSink) { data.forEach { sink.writeUByte(it) } }

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun UShortData.encode(sink: BufferedSink) { data.forEach { sink.writeUShort(it) } }

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun FunctionCode.encode(sink: BufferedSink) { sink.writeUByte(value) }
