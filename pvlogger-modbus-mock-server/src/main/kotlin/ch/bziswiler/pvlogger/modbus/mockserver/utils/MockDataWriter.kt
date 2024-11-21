package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.writeInt
import io.ktor.utils.io.core.writeShort
import io.ktor.utils.io.core.writeUByte
import io.ktor.utils.io.core.writeUInt
import io.ktor.utils.io.core.writeUShort
import io.ktor.utils.io.writePacket
import mu.KotlinLogging

class MockDataWriter(private val channel: ByteWriteChannel) {

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun writeData(header: MbapHeader, functionCode: FunctionCode, data: List<RegisterValue<*>>) {
        channel.writePacket {
            writeUShort(header.protocolIdentifier).also { logger.trace { "writing protocol identifier '${header.protocolIdentifier}'" } }
            writeUShort(header.protocolType).also { logger.trace { "writing protocol type identifier '${header.protocolType}'" } }
            writeUShort(header.dataLength).also { logger.trace { "writing data length '${header.dataLength}'" } }
            writeUByte(header.logicDeviceId).also { logger.trace { "writing logic device id '${header.logicDeviceId}'" } }
            writeUByte(functionCode.value).also { logger.trace { "writing function code '${functionCode.value}'" } }
            val numberOfBytes = header.dataLength.minus(3u).toUByte()
            writeUByte(numberOfBytes).also { logger.trace { "writing number of bytes '${numberOfBytes}'" } }
            data.forEach {
                val value = it.value
                when (it.type) {
                    Type.U16 -> writeUShort(value as UShort).also { logger.trace { "writing U16 '${value}'" } }
                    Type.I16 -> writeShort(value as Short).also { logger.trace { "writing I16 '${value}'" } }
                    Type.U32 -> writeUInt(value as UInt).also { logger.trace { "writing U32 '${value}'" } }
                    Type.I32 -> writeInt(value as Int).also { logger.trace { "writing I32 '${value}'" } }
                    Type.BITFIELD_16 -> writeUShort(value as UShort).also { logger.trace { "writing BITFIELD_16 '${value}'" } }
                    Type.BITFIELD_32 -> writeUInt(value as UInt).also { logger.trace { "writing BITFIELD_32 '${value}'" } }
                    else -> throw UnsupportedOperationException("Unknown type: ${it.type}")
                }
            }
        }
    }
}