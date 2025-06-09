package ch.bziswiler.pvlogger.datacollector.services.modbus.response

import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusRegisterValue
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.MbapHeader
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.RequestId
import java.time.LocalDateTime

data class ModbusRegisterValueResponse(
    override var jobId: JobId,
    override val requestId: RequestId,
    override var requestTime: LocalDateTime,
    override var responseTime: LocalDateTime,
    override var header: MbapHeader,
    override var functionCode: FunctionCode,
    override var numberOfBytes: UByte,
    var registerValues: List<ModbusRegisterValue<*>>,
) : ModbusResponse
