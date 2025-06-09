package ch.bziswiler.pvlogger.datacollector.services.modbus.request

import ch.bziswiler.pvlogger.datacollector.services.modbus.readMld
import ch.bziswiler.pvlogger.datacollector.services.modbus.readString
import ch.bziswiler.pvlogger.datacollector.services.modbus.readUInt
import ch.bziswiler.pvlogger.datacollector.services.modbus.readUShort
import ch.bziswiler.pvlogger.modbus.tcp.api.*
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.AduFrame
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.FunctionCode.ReadRegisters
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.MbapHeader
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.UShortData
import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.Type.*
import kotlinx.io.Sink
import kotlinx.io.Source
import java.time.LocalDateTime

class ReadRegistersValueRequest(
    override val jobId: String,
    override val requestTime: LocalDateTime,
    override val requestId: UShort,
    override val logicDeviceId: UByte,
    private val registers: List<ModbusRegisterDefinition>,
) : ModbusRegisterValueRequest() {
    override val dataLength: UShort = 0x6u
    override val functionCode = ReadRegisters

    private val registerAddress: UShort = registers.firstOrNull()?.registerAddress?.value?.toUShort() ?: 0x0u
    private val numberOfRegistersInBytes: UShort = registers.sumOf { it.quantity.value }.toUShort()

    private val frame =
        AduFrame(
            MbapHeader(requestId, dataLength = dataLength, logicDeviceId = logicDeviceId),
            functionCode,
            UShortData(listOf(registerAddress, numberOfRegistersInBytes)),
        )

    override suspend fun encode(channel: Sink) = frame.encode(channel)

    override suspend fun readRegisters(channel: Source) =
        registers.fold(listOf<ModbusRegisterValue<*>>()) { list, register ->
            list.plus(readRegisterValue(register, channel))
        }

    private fun readRegisterValue(register: ModbusRegisterDefinition, channel: Source) =
        when (register.type) {
            U16 -> ModbusRegisterUShortValue(register, channel.readUShort())
            U32 -> ModbusRegisterUIntValue(register, channel.readUInt())
            I16 -> ModbusRegisterShortValue(register, channel.readShort())
            I32 -> ModbusRegisterIntValue(register, channel.readInt())
            STR -> ModbusRegisterStringValue(register, channel.readString(toNOfBytes(register.quantity.value)))
            MLD -> ModbusRegisterShortArrayValue(register, channel.readMld(register.quantity.value))
            BITFIELD_16 -> ModbusRegisterUShortValue(register, channel.readUShort())
            BITFIELD_32 -> ModbusRegisterUIntValue(register, channel.readUInt())
        }

    private fun toNOfBytes(nOfShorts: Int) = 2 * nOfShorts
}
