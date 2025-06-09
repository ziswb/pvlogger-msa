package ch.bziswiler.pvlogger.datacollector.services.modbus.response

import ch.bziswiler.pvlogger.modbus.tcp.api.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.MbapHeader
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.RequestId
import java.io.Serializable
import java.time.LocalDateTime

interface ModbusResponse: Serializable {
    var jobId: JobId
    val requestId: RequestId
    var requestTime: LocalDateTime
    var responseTime: LocalDateTime
    var header: MbapHeader
    var functionCode: FunctionCode
    var numberOfBytes: UByte
}
