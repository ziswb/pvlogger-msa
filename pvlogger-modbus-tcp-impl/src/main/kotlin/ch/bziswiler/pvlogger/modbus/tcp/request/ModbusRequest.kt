package ch.bziswiler.pvlogger.modbus.tcp.request

import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.modbus.tcp.readUByte
import ch.bziswiler.pvlogger.modbus.tcp.readUShort
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse
import kotlinx.serialization.Serializable
import okio.BufferedSink
import okio.BufferedSource
import java.time.LocalDateTime

@Serializable
interface ModbusRequest {
    val jobId: String
    val requestTime: LocalDateTime
    val requestId: UShort
    val dataLength: UShort
    val logicDeviceId: UByte
    val functionCode: FunctionCode

    suspend fun encode(sink: BufferedSink)

    suspend fun decode(source: BufferedSource): ModbusResponse

    suspend fun readHeader(source: BufferedSource) =
        MbapHeader(
            protocolIdentifier = source.readUShort(),
            protocolType = source.readUShort(),
            dataLength = source.readUShort(),
            logicDeviceId = source.readUByte(),
        )
}
