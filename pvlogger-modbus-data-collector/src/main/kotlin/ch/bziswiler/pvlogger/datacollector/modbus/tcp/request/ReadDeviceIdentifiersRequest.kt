package ch.bziswiler.pvlogger.datacollector.modbus.tcp.request

import ch.bziswiler.pvlogger.datacollector.modbus.tcp.decodeModbusObject
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.frame.AduFrame
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.readUByte
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.RequestId
import ch.bziswiler.pvlogger.shared.modbus.ModbusObject
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode.Companion.from
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode.ReadDeviceIdentifiers
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.UByteData
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ReadDeviceIdentifiersResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import mu.KotlinLogging
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

    override suspend fun encode(channel: ByteWriteChannel) = frame.encode(channel)

    override suspend fun decode(channel: ByteReadChannel): ReadDeviceIdentifiersResponse =
        run {
            logger.debug("ReadDeviceIdentifiersRequest.decode()")
            val header = readHeader(channel)
            val functionCode = from(channel.readUByte())
            val meiType = channel.readUByte()
            val readDevId = channel.readUByte()
            val consistencyLevel = channel.readUByte()
            val more = channel.readUByte()
            val nextObjectId = channel.readUByte()
            val numberOfObjects = channel.readUByte()
            val objectList = mutableListOf<ModbusObject>()
            repeat(numberOfObjects.toInt()) { objectList.add(decodeModbusObject(channel)) }
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
