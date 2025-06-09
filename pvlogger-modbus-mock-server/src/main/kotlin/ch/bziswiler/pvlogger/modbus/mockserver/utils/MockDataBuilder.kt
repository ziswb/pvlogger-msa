package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.modbus.tcp.api.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.api.tcp.frame.MbapHeader
import mu.KotlinLogging

object MockDataBuilder {
    fun buildHeader(
        protocolIdentifier: UShort = 0u,
        protocolType: UShort = 0u,
        dataLength: UShort = 81u,
        logicDeviceId: UByte = 1u
    ) = MbapHeader(
        protocolIdentifier = protocolIdentifier,
        protocolType = protocolType,
        dataLength = dataLength,
        logicDeviceId = logicDeviceId
    )

    private val uintRange = UIntRange(0u, 1000u);
    private val uShortRange = UIntRange(0u, 100u);
    private val intRange = IntRange(-1000, 1000);
    private val shortRange = IntRange(-100, 100);

    fun buildFunctionCode(code: UByte = 0x03u) = FunctionCode.from(code)

    fun buildData() = listOf(
        RegisterUShortValue(value = 1u),
        RegisterIntValue(value = 2333),
        RegisterIntValue(value = 2416),
        RegisterIntValue(value = 2366),
        RegisterIntValue(value = -257),
        RegisterIntValue(value = -246),
        RegisterIntValue(value = -622),
        RegisterIntValue(value = -2401),
        RegisterIntValue(value = -968),
        RegisterShortValue(value = 896),
        RegisterShortValue(value = 4997),
        RegisterIntValue(value = 1295013),
        RegisterIntValue(value = 798831),
        RegisterIntValue(value = 0),
        RegisterUShortValue(value = 1u),
        RegisterIntValue(value = 4113),
        RegisterIntValue(value = 4142),
        RegisterIntValue(value = 4071),
        RegisterIntValue(value = -427),
        RegisterIntValue(value = -569),
        RegisterIntValue(value = -1405),
        RegisterUShortValue(value = 1u)
    )

    suspend fun buildDataUsingMasterData(registerAddress: RegisterAddress, numberOfBytes: Int): List<RegisterValue<*>> {
        val logger = KotlinLogging.logger {}
        val values = mutableListOf<RegisterValue<*>>()
        var currentRegisterAddress = registerAddress
        var bytesWritten = 0
        while (bytesWritten < numberOfBytes) {
            logger.trace { "Written bytes: $bytesWritten - looking up register definition for register address '${currentRegisterAddress.value}'" }
            val registerDefinition = MasterDataCache.modbusRegisterDefinition(currentRegisterAddress)
            logger.trace { "Found register definition: $registerDefinition" }
            values.add(mockValueFor(registerDefinition))
            bytesWritten += registerDefinition.quantity.value
            currentRegisterAddress = RegisterAddress(currentRegisterAddress.value + registerDefinition.quantity.value)
        }
        return values
    }

    private fun mockValueFor(registerDefinition: ModbusRegisterDefinition): RegisterValue<*> = when (registerDefinition.type) {
        Type.U16 -> RegisterUShortValue(value = uShortRange.random().toUShort())
        Type.I16 -> RegisterShortValue(value = shortRange.random().toShort())
        Type.U32 -> RegisterUIntValue(value = uintRange.random())
        Type.I32 -> RegisterIntValue(value = intRange.random())
        Type.BITFIELD_16 -> RegisterUShortValue(value = uShortRange.random().toUShort())
        Type.BITFIELD_32 -> RegisterUIntValue(value = intRange.random().toUInt())
        else -> throw NotImplementedError()
    }
}