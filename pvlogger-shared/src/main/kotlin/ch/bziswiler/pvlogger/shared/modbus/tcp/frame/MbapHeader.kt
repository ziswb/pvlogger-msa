package ch.bziswiler.pvlogger.shared.modbus.tcp.frame

import kotlinx.serialization.Serializable

private const val TWO_BYTE = 0xFFFF

private const val ONE_BYTE = 0xFF

/*

    The kotlin.UByte is an unsigned 8-bit integer (0 – 255)
    The kotlin.UShort is an unsigned 16-bit integer (0 – 65535)
    The kotlin.UInt is an unsigned 32-bit integer (0 to 2^32 – 1)
    The kotlin.ULong is an unsigned 64-bit integer (0 to 2^64 -1)

 */
@Serializable
class MbapHeader(
    var protocolIdentifier: UShort,
    var protocolType: UShort = 0x0u,
    var dataLength: UShort,
    var logicDeviceId: UByte,
)
