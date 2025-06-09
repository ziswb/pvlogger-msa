package ch.bziswiler.pvlogger.datacollector.services.modbus.request

import ch.bziswiler.pvlogger.datacollector.services.modbus.readUByte
import ch.bziswiler.pvlogger.datacollector.services.modbus.readUShort
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.MbapHeader
import ch.bziswiler.pvlogger.datacollector.services.modbus.response.ModbusResponse
import kotlinx.io.Sink
import kotlinx.io.Source
import java.time.LocalDateTime

interface ModbusRequest {
    val jobId: String
    val requestTime: LocalDateTime
    val requestId: UShort
    val dataLength: UShort
    val logicDeviceId: UByte
    val functionCode: FunctionCode

    suspend fun encode(channel: Sink)

    suspend fun decode(channel: Source): ModbusResponse

    suspend fun readHeader(channel: Source) =
        MbapHeader(
            protocolIdentifier = channel.readUShort(),
            protocolType = channel.readUShort(),
            dataLength = channel.readUShort(),
            logicDeviceId = channel.readUByte(),
        )
}
