package ch.bziswiler.pvlogger.modbus.api.tcp

import ch.bziswiler.pvlogger.modbus.api.meta.JobId
import ch.bziswiler.pvlogger.modbus.api.meta.RequestId
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
interface ModbusResponse {
    var jobId: JobId
    val requestId: RequestId
    var requestTime: LocalDateTime
    var responseTime: LocalDateTime
    var header: MbapHeader
    var functionCode: FunctionCode
    var numberOfBytes: UByte
}