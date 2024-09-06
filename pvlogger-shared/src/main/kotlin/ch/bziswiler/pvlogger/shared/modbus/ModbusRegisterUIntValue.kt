package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

data class ModbusRegisterUIntValue(override var definition: ModbusRegisterDefinition, var value: UInt) :
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
        this.value = ois.readInt().toUInt()
    }
}
