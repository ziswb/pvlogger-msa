package ch.bziswiler.pvlogger.modbus.tcp.frame

import kotlinx.serialization.Serializable
import java.io.ObjectInputStream

@Serializable
interface Data<T> {
    var data: List<T>

    fun readData(ois: ObjectInputStream): T
}
