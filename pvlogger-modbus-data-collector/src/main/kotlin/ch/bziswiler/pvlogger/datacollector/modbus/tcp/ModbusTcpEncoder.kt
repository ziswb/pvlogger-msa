package ch.bziswiler.pvlogger.datacollector.modbus.tcp

import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.UByteData
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.UShortData
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.writeUByte
import io.ktor.utils.io.core.writeUShort
import io.ktor.utils.io.writePacket

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun MbapHeader.encode(channel: ByteWriteChannel) {
    channel.writePacket {
        writeUShort(protocolIdentifier)
        writeUShort(protocolType)
        writeUShort(dataLength)
        writeUByte(logicDeviceId)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun UByteData.encode(channel: ByteWriteChannel) = channel.writePacket { data.forEach { writeUByte(it) } }

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun UShortData.encode(channel: ByteWriteChannel) = channel.writePacket { data.forEach { writeUShort(it) } }


@OptIn(ExperimentalUnsignedTypes::class)
suspend fun FunctionCode.encode(channel: ByteWriteChannel) = channel.writePacket { writeUByte(value) }
