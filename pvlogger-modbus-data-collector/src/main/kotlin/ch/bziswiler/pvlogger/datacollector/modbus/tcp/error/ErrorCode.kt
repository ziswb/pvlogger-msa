package ch.bziswiler.pvlogger.datacollector.modbus.tcp.error

open class ErrorCode(val value: Short) {
    object IllegalFunction : ErrorCode(ErrorCodes.ILLEGAL_FUNCTION.value)

    object IllegalDataAccess : ErrorCode(ErrorCodes.ILLEGAL_DATA_ACCESS.value)

    object IllegalDataValue : ErrorCode(ErrorCodes.ILLEGAL_DATA_VALUE.value)

    object SlaveNodeFailure : ErrorCode(ErrorCodes.SLAVE_NODE_FAILURE.value)

    object SlaveDeviceBusy : ErrorCode(ErrorCodes.SLAVE_DEVICE_BUSY.value)

    object NoPermission : ErrorCode(ErrorCodes.NO_PERMISSION.value)

    private enum class ErrorCodes(val value: Short) {
        ILLEGAL_FUNCTION(0x01),
        ILLEGAL_DATA_ACCESS(0x02),
        ILLEGAL_DATA_VALUE(0x03),
        SLAVE_NODE_FAILURE(0x04),
        SLAVE_DEVICE_BUSY(0x06),
        NO_PERMISSION(0x80),
    }
}
