package ch.bziswiler.pvlogger.modbus.tcp.api

@OptIn(ExperimentalUnsignedTypes::class)
data class ModbusObject(
    val objectId: UByte,
    val objectLength: UByte,
    val objectValue: UByteArray,
) {
    override fun toString(): String =
        """
        Object ID: $objectId
        Object Length: $objectLength
        Object Value: ${objectValue.toByteArray().decodeToString()}
        """

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModbusObject

        if (objectId != other.objectId) return false
        if (objectLength != other.objectLength) return false
        if (!objectValue.contentEquals(other.objectValue)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = objectId.hashCode()
        result = 31 * result + objectLength.hashCode()
        result = 31 * result + objectValue.contentHashCode()
        return result
    }
}
