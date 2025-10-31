package ch.bziswiler.pvlogger.modbus.tcp.request

import ch.bziswiler.pvlogger.modbus.api.ModbusRegisterValue
import ch.bziswiler.pvlogger.modbus.api.masterdata.Type
import ch.bziswiler.pvlogger.modbus.api.masterdata.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.modbus.api.tcp.FunctionCode
import ch.bziswiler.pvlogger.modbus.api.tcp.MbapHeader
import ch.bziswiler.pvlogger.modbus.api.tcp.readUInt
import ch.bziswiler.pvlogger.modbus.api.tcp.readUShort
import ch.bziswiler.pvlogger.modbus.tcp.ModbusRegisterIntValue
import ch.bziswiler.pvlogger.modbus.tcp.ModbusRegisterShortArrayValue
import ch.bziswiler.pvlogger.modbus.tcp.ModbusRegisterShortValue
import ch.bziswiler.pvlogger.modbus.tcp.ModbusRegisterStringValue
import ch.bziswiler.pvlogger.modbus.tcp.ModbusRegisterUIntValue
import ch.bziswiler.pvlogger.modbus.tcp.ModbusRegisterUShortValue
import ch.bziswiler.pvlogger.modbus.tcp.frame.AduFrame
import ch.bziswiler.pvlogger.modbus.tcp.frame.UShortData
import ch.bziswiler.pvlogger.modbus.tcp.readMld
import ch.bziswiler.pvlogger.modbus.tcp.readString
import okio.BufferedSink
import okio.BufferedSource
import java.time.LocalDateTime

class ReadRegistersValueRequest(
    override val jobId: String,
    override val requestTime: LocalDateTime,
    override val requestId: UShort,
    override val logicDeviceId: UByte,
    private val registers: List<ModbusRegisterDefinition>,
) : ModbusRegisterValueRequest() {
    override val dataLength: UShort = 0x6u
    override val functionCode = FunctionCode.ReadRegisters

    private val registerAddress: UShort =
        registers
            .firstOrNull()
            ?.registerAddress
            ?.value
            ?.toUShort() ?: 0x0u
    private val numberOfRegistersInBytes: UShort = registers.sumOf { it.quantity.value }.toUShort()

    private val frame =
        AduFrame(
            MbapHeader(requestId, dataLength = dataLength, logicDeviceId = logicDeviceId),
            functionCode,
            UShortData(listOf(registerAddress, numberOfRegistersInBytes)),
        )

    override suspend fun encode(sink: BufferedSink) = frame.encode(sink)

    override suspend fun readRegisters(channel: BufferedSource) =
        registers.fold(listOf<ModbusRegisterValue<*>>()) { list, register ->
            list.plus(readRegisterValue(register, channel))
        }

    private suspend fun readRegisterValue(
        register: ModbusRegisterDefinition,
        channel: BufferedSource,
    ) = when (register.type) {
        Type.U16 -> ModbusRegisterUShortValue(register, channel.readUShort())
        Type.U32 -> ModbusRegisterUIntValue(register, channel.readUInt())
        Type.I16 -> ModbusRegisterShortValue(register, channel.readShort())
        Type.I32 -> ModbusRegisterIntValue(register, channel.readInt())
        Type.STR ->
            ModbusRegisterStringValue(
                register,
                channel.readString(toNOfBytes(register.quantity.value)),
            )

        Type.MLD -> ModbusRegisterShortArrayValue(register, channel.readMld(register.quantity.value))
        Type.BITFIELD_16 -> ModbusRegisterUShortValue(register, channel.readUShort())
        Type.BITFIELD_32 -> ModbusRegisterUIntValue(register, channel.readUInt())
    }

    private fun toNOfBytes(nOfShorts: Int) = 2 * nOfShorts
}
