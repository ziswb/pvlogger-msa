package ch.bziswiler.pvlogger.datacollector.modbus.tcp.request

import ch.bziswiler.pvlogger.datacollector.modbus.tcp.readUByte
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.readUShort
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import java.time.LocalDateTime

interface ModbusRequest {
    val jobId: String
    val requestTime: LocalDateTime
    val requestId: UShort
    val dataLength: UShort
    val logicDeviceId: UByte
    val functionCode: FunctionCode

    suspend fun encode(channel: ByteWriteChannel)

    suspend fun decode(channel: ByteReadChannel): ModbusResponse

    suspend fun readHeader(channel: ByteReadChannel) =
        MbapHeader(
            protocolIdentifier = channel.readUShort(),
            protocolType = channel.readUShort(),
            dataLength = channel.readUShort(),
            logicDeviceId = channel.readUByte(),
        )
}
