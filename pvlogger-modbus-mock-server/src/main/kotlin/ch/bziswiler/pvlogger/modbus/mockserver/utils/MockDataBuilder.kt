package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.frame.MbapHeader

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
}