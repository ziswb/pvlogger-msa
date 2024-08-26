package ch.bziswiler.pvlogger.modbus.tcp.frame

import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.FunctionCodes.ABNORMAL_RESPONSE
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.FunctionCodes.READ_DEVICE_IDENTIFIERS
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.FunctionCodes.READ_REGISTERS
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.FunctionCodes.UNKNOWN
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.FunctionCodes.WRITE_MULTIPLE_REGISTERS
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.FunctionCodes.WRITE_SINGLE_REGISTER
import java.io.Serializable

open class FunctionCode(val value: UByte) : Serializable {
    object ReadRegisters : FunctionCode(READ_REGISTERS.value) {
        private fun readResolve(): Any = ReadRegisters
    }

    object WriteSingleRegister : FunctionCode(WRITE_SINGLE_REGISTER.value) {
        private fun readResolve(): Any = WriteSingleRegister
    }

    object WriteMultipleRegisters : FunctionCode(WRITE_MULTIPLE_REGISTERS.value) {
        private fun readResolve(): Any = WriteMultipleRegisters
    }

    object ReadDeviceIdentifiers : FunctionCode(READ_DEVICE_IDENTIFIERS.value) {
        private fun readResolve(): Any = ReadDeviceIdentifiers
    }

    object AbnormalResponse : FunctionCode(ABNORMAL_RESPONSE.value) {
        private fun readResolve(): Any = AbnormalResponse
    }

    object UnknownResponse : FunctionCode(UNKNOWN.value) {
        private fun readResolve(): Any = UnknownResponse
    }

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
