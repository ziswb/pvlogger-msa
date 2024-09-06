package ch.bziswiler.pvlogger.shared.modbus

import java.io.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
data class ModbusObject(
    val objectId: UByte,
    val objectLength: UByte,
    val objectValue: UByteArray,
) : Serializable {
    override fun toString(): String =
        """
        Object ID: $objectId
        Object Length: $objectLength
        Object Value: ${objectValue.toByteArray().decodeToString()}
        """
}
