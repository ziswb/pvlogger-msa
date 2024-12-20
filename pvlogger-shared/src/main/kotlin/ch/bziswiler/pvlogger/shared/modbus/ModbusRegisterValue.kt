package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

interface ModbusRegisterValue<T> : Serializable {

    var definition: ModbusRegisterDefinition
    var value: T

    @Throws(IOException::class)
    @Suppress("unused")
    fun writeObject(oos: ObjectOutputStream) {
        oos.writeObject(definition)
        oos.writeObject(value)
    }

    @Throws(IOException::class)
    @Suppress("unused")
    fun readObject(ois: ObjectInputStream) {
        this.definition = ois.readObject() as ModbusRegisterDefinition
        @Suppress("UNCHECKED_CAST")
        this.value = ois.readObject() as T
    }
}
