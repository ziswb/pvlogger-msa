package ch.bziswiler.pvlogger.datacollector.jobs.modbus.tcp.request

import ch.bziswiler.pvlogger.datacollector.jobs.modbus.tcp.decodeModbusObject
import ch.bziswiler.pvlogger.datacollector.jobs.modbus.tcp.frame.AduFrame
import ch.bziswiler.pvlogger.datacollector.jobs.modbus.tcp.readUByte
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.RequestId
import ch.bziswiler.pvlogger.shared.modbus.ModbusObject
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode.Companion.from
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode.ReadDeviceIdentifiers
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.UByteData
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ReadDeviceIdentifiersResponse
import mu.KotlinLogging
import okio.BufferedSink
import okio.BufferedSource
import java.time.LocalDateTime
import java.time.LocalDateTime.now

open class ReadDeviceIdentifiersRequest(
    final override val jobId: String,
    final override val requestTime: LocalDateTime,
    final override val requestId: UShort,
    final override val logicDeviceId: UByte,
    final override val dataLength: UShort = 0x05u,
    final override val functionCode: FunctionCode = ReadDeviceIdentifiers,
    meiType: UByte = 0x0Eu,
    readDevIdCode: UByte = 0x01u,
    objectId: UByte = 0x00u,
) : ModbusRequest {

    private val logger = KotlinLogging.logger {}

    private val frame =
        AduFrame(
            MbapHeader(requestId, dataLength = dataLength, logicDeviceId = logicDeviceId),
            functionCode,
            UByteData(listOf(meiType, readDevIdCode, objectId)),
        )

    override suspend fun encode(sink: BufferedSink) = frame.encode(sink)

    override suspend fun decode(source: BufferedSource): ReadDeviceIdentifiersResponse =
        run {
            logger.debug("ReadDeviceIdentifiersRequest.decode()")
            val header = readHeader(source)
            val functionCode = from(source.readUByte())
            val meiType = source.readUByte()
            val readDevId = source.readUByte()
            val consistencyLevel = source.readUByte()
            val more = source.readUByte()
            val nextObjectId = source.readUByte()
            val numberOfObjects = source.readUByte()
            val objectList = mutableListOf<ModbusObject>()
            repeat(numberOfObjects.toInt()) { objectList.add(decodeModbusObject(source)) }
            ReadDeviceIdentifiersResponse(
                jobId = JobId(jobId),
                requestId = RequestId(requestId),
                requestTime = requestTime,
                responseTime = now(),
                header = header,
                functionCode = functionCode,
                numberOfBytes = header.dataLength.toInt().minus(3).toUByte(),
                meiType = meiType,
                readDevIdCode = readDevId,
                consistencyLevel = consistencyLevel,
                more = more,
                nextObjectId = nextObjectId,
                numberOfObjects = numberOfObjects,
                objectList = objectList,
            )
        }
}
