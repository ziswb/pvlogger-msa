package ch.bziswiler.pvlogger.modbus.tcp.request

import ch.bziswiler.pvlogger.modbus.api.tcp.FunctionCode
import java.time.LocalDateTime

class ReadDeviceListRequest(
    jobId: String,
    requestTime: LocalDateTime,
    requestId: UShort,
    logicDeviceId: UByte,
) : ReadDeviceIdentifiersRequest(
        jobId,
        requestTime,
        requestId,
        logicDeviceId,
        dataLength = 0x5u,
        functionCode = FunctionCode.ReadDeviceIdentifiers,
        meiType = 0x0Eu,
        readDevIdCode = 0x03u,
        objectId = 0x87u,
    )
