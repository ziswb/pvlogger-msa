package ch.bziswiler.pvlogger.modbus.tcp.api.frame

import ch.bziswiler.pvlogger.modbus.tcp.api.frame.FunctionCode.FunctionCodes.*

open class FunctionCode(val value: UByte) {

    object ReadRegisters : FunctionCode(READ_REGISTERS.value)

    object WriteSingleRegister : FunctionCode(WRITE_SINGLE_REGISTER.value)

    object WriteMultipleRegisters : FunctionCode(WRITE_MULTIPLE_REGISTERS.value)

    object ReadDeviceIdentifiers : FunctionCode(READ_DEVICE_IDENTIFIERS.value)

    object AbnormalResponse : FunctionCode(ABNORMAL_RESPONSE.value)

    object UnknownResponse : FunctionCode(UNKNOWN.value)

    private enum class FunctionCodes(val value: UByte) {
        READ_REGISTERS(0x03u),
        WRITE_SINGLE_REGISTER(0x06u),
        WRITE_MULTIPLE_REGISTERS(0x10u),
        READ_DEVICE_IDENTIFIERS(0x2Bu),
        ABNORMAL_RESPONSE(0x83u),
        UNKNOWN(0x0u),
    }

    companion object {
        fun from(rawFunctionCode: UByte) =
            when (rawFunctionCode) {
                READ_REGISTERS.value -> ReadRegisters
                WRITE_SINGLE_REGISTER.value -> WriteSingleRegister
                WRITE_MULTIPLE_REGISTERS.value -> WriteMultipleRegisters
                READ_DEVICE_IDENTIFIERS.value -> ReadDeviceIdentifiers
                ABNORMAL_RESPONSE.value -> AbnormalResponse
                else -> UnknownResponse
            }
    }
}
