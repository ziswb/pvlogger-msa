package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeBuffer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.io.Buffer
import kotlinx.io.writeUByte
import kotlinx.io.writeUInt
import kotlinx.io.writeUShort

class MockDataWriter(private val channel: ByteWriteChannel) {

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun writeData(header: MbapHeader, functionCode: FunctionCode, data: List<RegisterValue<*>>) {
        val buffer = Buffer()
        buffer.writeUShort(header.protocolIdentifier).also { logger.trace { "writing protocol identifier '${header.protocolIdentifier}'" } }
        buffer.writeUShort(header.protocolType).also { logger.trace { "writing protocol type identifier '${header.protocolType}'" } }
        buffer.writeUShort(header.dataLength).also { logger.trace { "writing data length '${header.dataLength}'" } }
        buffer.writeUByte(header.logicDeviceId).also { logger.trace { "writing logic device id '${header.logicDeviceId}'" } }
        buffer.writeUByte(functionCode.value).also { logger.trace { "writing function code '${functionCode.value}'" } }
        val numberOfBytes = header.dataLength.minus(3u).toUByte()
        buffer.writeUByte(numberOfBytes).also { logger.trace { "writing number of bytes '${numberOfBytes}'" } }
        data.forEach {
            val value = it.value
            when (it.type) {
                Type.U16 -> buffer.writeUShort(value as UShort).also { logger.trace { "writing U16 '${value}'" } }
                Type.I16 -> buffer.writeShort(value as Short).also { logger.trace { "writing I16 '${value}'" } }
                Type.U32 -> buffer.writeUInt(value as UInt).also { logger.trace { "writing U32 '${value}'" } }
                Type.I32 -> buffer.writeInt(value as Int).also { logger.trace { "writing I32 '${value}'" } }
                Type.BITFIELD_16 -> buffer.writeUShort(value as UShort).also { logger.trace { "writing BITFIELD_16 '${value}'" } }
                Type.BITFIELD_32 -> buffer.writeUInt(value as UInt).also { logger.trace { "writing BITFIELD_32 '${value}'" } }
                else -> throw UnsupportedOperationException("Unknown type: ${it.type}")
            }
        }
        channel.writeBuffer(buffer)
        channel.flush()
    }
}
