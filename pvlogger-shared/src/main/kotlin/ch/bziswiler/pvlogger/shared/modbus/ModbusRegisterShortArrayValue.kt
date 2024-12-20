package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

data class ModbusRegisterShortArrayValue(override var definition: ModbusRegisterDefinition, override var value: ShortArray) :
    ModbusRegisterValue<ShortArray> {

    @Throws(IOException::class)
    @Suppress("unused")
    override fun writeObject(oos: ObjectOutputStream) {
        super.writeObject(oos)
        oos.writeObject(value)
    }

    @Throws(IOException::class)
    @Suppress("unused")
    override fun readObject(ois: ObjectInputStream) {
        super.readObject(ois)
        this.value = ois.readObject() as ShortArray
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModbusRegisterShortArrayValue

        if (definition != other.definition) return false
        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = definition.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}
