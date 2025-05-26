package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

private const val FOUR_BYTES = 0xFFFFFFFF

data class ModbusRegisterUIntValue(override var definition: ModbusRegisterDefinition, override var value: UInt) :
    ModbusRegisterValue<UInt> {

    @Throws(IOException::class)
    @Suppress("unused")
    override fun writeObject(oos: ObjectOutputStream) {
        super.writeObject(oos)
        oos.writeLong(value.toLong())
    }

    @Throws(IOException::class)
    @Suppress("unused")
    override fun readObject(ois: ObjectInputStream) {
        super.readObject(ois)
        this.value = (ois.readLong() and FOUR_BYTES).toUInt()
    }
}
