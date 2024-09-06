package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.writeInt
import io.ktor.utils.io.core.writeShort
import io.ktor.utils.io.core.writeUByte
import io.ktor.utils.io.core.writeUShort
import io.ktor.utils.io.writePacket

class MockDataWriter(private val channel: ByteWriteChannel) {

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun writeData(header: MbapHeader, functionCode: FunctionCode, data: List<RegisterValue<*>> = emptyList()) {
        channel.writePacket {
            writeUShort(header.protocolIdentifier)
            writeUShort(header.protocolType)
            writeUShort(header.dataLength)
            writeUByte(header.logicDeviceId)
        }
        channel.writePacket { writeUByte(functionCode.value) }
        channel.writePacket {
            data.forEach {
                when (it.type) {
                    Type.U16 -> writeUShort(it.value as UShort)
                    Type.I16 -> writeShort(it.value as Short)
                    Type.I32 -> writeInt(it.value as Int)
                    else -> throw UnsupportedOperationException("Unknown type: ${it.type}")
                }
            }
        }
    }
}