package ch.bziswiler.pvlogger.datacollector.modbus.tcp.request

import ch.bziswiler.pvlogger.datacollector.modbus.tcp.frame.AduFrame
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.readMld
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.readString
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.readUInt
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.readUShort
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type.BITFIELD_16
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type.BITFIELD_32
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type.I16
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type.I32
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type.MLD
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type.STR
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type.U16
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type.U32
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterIntValue
import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterShortArrayValue
import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterShortValue
import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterStringValue
import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterUIntValue
import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterUShortValue
import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterValue
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode.ReadRegisters
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.UShortData
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
    override val functionCode = ReadRegisters

    private val registerAddress: UShort = registers.firstOrNull()?.registerAddress?.value?.toUShort() ?: 0x0u
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

    private suspend fun readRegisterValue(register: ModbusRegisterDefinition, channel: BufferedSource) =
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
