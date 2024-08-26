package ch.bziswiler.pvlogger.modbus

import ch.bziswiler.pvlogger.masterdata.modbus.dto.ModbusRegisterDefinition
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

interface ModbusRegisterValue : Serializable {

    var definition: ModbusRegisterDefinition

    @Throws(IOException::class)
    @Suppress("unused")
    fun writeObject(oos: ObjectOutputStream) {
        oos.writeObject(definition)
    }

    @Throws(IOException::class)
    @Suppress("unused")
    fun readObject(ois: ObjectInputStream) {
        this.definition = ois.readObject() as ModbusRegisterDefinition
    }
}
