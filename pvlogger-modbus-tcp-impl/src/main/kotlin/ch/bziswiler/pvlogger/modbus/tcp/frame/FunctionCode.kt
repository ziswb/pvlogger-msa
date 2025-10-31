package ch.bziswiler.pvlogger.modbus.tcp.frame

import kotlinx.serialization.Serializable

@Serializable
open class FunctionCode(
    val value: UByte,
) {
    object ReadRegisters : FunctionCode(FunctionCodes.READ_REGISTERS.value) {
        private fun readResolve(): Any = ReadRegisters
    }

    object WriteSingleRegister : FunctionCode(FunctionCodes.WRITE_SINGLE_REGISTER.value) {
        private fun readResolve(): Any = WriteSingleRegister
    }

    object WriteMultipleRegisters : FunctionCode(FunctionCodes.WRITE_MULTIPLE_REGISTERS.value) {
        private fun readResolve(): Any = WriteMultipleRegisters
    }

    object ReadDeviceIdentifiers : FunctionCode(FunctionCodes.READ_DEVICE_IDENTIFIERS.value) {
        private fun readResolve(): Any = ReadDeviceIdentifiers
    }

    object AbnormalResponse : FunctionCode(FunctionCodes.ABNORMAL_RESPONSE.value) {
        private fun readResolve(): Any = AbnormalResponse
    }

    object UnknownResponse : FunctionCode(FunctionCodes.UNKNOWN.value) {
        private fun readResolve(): Any = UnknownResponse
    }

    private enum class FunctionCodes(
        val value: UByte,
    ) {
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
                FunctionCodes.READ_REGISTERS.value -> ReadRegisters
                FunctionCodes.WRITE_SINGLE_REGISTER.value -> WriteSingleRegister
                FunctionCodes.WRITE_MULTIPLE_REGISTERS.value -> WriteMultipleRegisters
                FunctionCodes.READ_DEVICE_IDENTIFIERS.value -> ReadDeviceIdentifiers
                FunctionCodes.ABNORMAL_RESPONSE.value -> AbnormalResponse
                else -> UnknownResponse
            }
    }
}
