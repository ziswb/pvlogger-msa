package ch.bziswiler.pvlogger.datacollector.modbus.tcp.request

import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.ReadDeviceIdentifiers
import java.time.LocalDateTime

class ReadDeviceListRequest(jobId: String, requestTime: LocalDateTime, requestId: UShort, logicDeviceId: UByte) :
    ReadDeviceIdentifiersRequest(
        jobId,
        requestTime,
        requestId,
        logicDeviceId,
        dataLength = 0x5u,
        functionCode = ReadDeviceIdentifiers,
        meiType = 0x0Eu,
        readDevIdCode = 0x03u,
        objectId = 0x87u,
    )
