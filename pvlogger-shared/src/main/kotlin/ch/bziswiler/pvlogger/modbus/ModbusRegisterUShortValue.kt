package ch.bziswiler.pvlogger.modbus

import ch.bziswiler.pvlogger.masterdata.modbus.dto.ModbusRegisterDefinition
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

private const val TWO_BYTES = 0xFFFF

data class ModbusRegisterUShortValue(override var definition: ModbusRegisterDefinition, var value: UShort) :
    ModbusRegisterValue {

    @Throws(IOException::class)
    @Suppress("unused")
    override fun writeObject(oos: ObjectOutputStream) {
        super.writeObject(oos)
        oos.writeInt(value.toInt())
    }

    @Throws(IOException::class)
    @Suppress("unused")
    override fun readObject(ois: ObjectInputStream) {
        super.readObject(ois)
        this.value = (ois.readInt() and TWO_BYTES).toUShort()
    }
}
