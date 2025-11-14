package ch.bziswiler.pvlogger.shared.modbus.tcp.response

import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.RequestId
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

private const val ONE_BYTE = 0xFF

@Serializable
sealed interface ModbusResponse {
    var jobId: JobId
    val requestId: RequestId
    var requestTime: LocalDateTime
    var responseTime: LocalDateTime
    var header: MbapHeader
    var functionCode: FunctionCode
    var numberOfBytes: UByte
}
