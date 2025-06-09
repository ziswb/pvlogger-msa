package ch.bziswiler.pvlogger.datacollector.services.modbus.response

import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusObject
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.MbapHeader
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.RequestId
import java.time.LocalDateTime

data class ReadDeviceIdentifiersResponse(
    override var jobId: JobId,
    override val requestId: RequestId,
    override var requestTime: LocalDateTime,
    override var responseTime: LocalDateTime,
    override var header: MbapHeader,
    override var functionCode: FunctionCode,
    override var numberOfBytes: UByte,
    var meiType: UByte = 0x0Eu,
    var readDevIdCode: UByte = 0x01u,
    var consistencyLevel: UByte = 0x01u,
    var more: UByte,
    var nextObjectId: UByte,
    var numberOfObjects: UByte,
    var objectList: List<ModbusObject>,
) : ModbusResponse {

    override fun toString(): String =
        """
        MEI Type: $meiType
        Read Device ID: $readDevIdCode
        Conformity Level: $consistencyLevel
        More Follows: $more
        Next Object ID: $nextObjectId
        Number of Objects: $numberOfObjects
        Objects: ${objectList.joinToString()}
        """
}
