package ch.bziswiler.pvlogger.shared.modbus.tcp.frame

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

interface Data<T> : Serializable {
    var data: List<T>

    @Throws(IOException::class)
    @Suppress("unused")
    fun writeObject(oos: ObjectOutputStream) {
        oos.writeInt(data.size)
        data.forEach {
            if (it is Number) {
                oos.writeInt(it.toInt())
            } else {
                oos.writeObject(it)
            }
        }
    }

    @Throws(IOException::class)
    @Suppress("unused")
    fun readObject(ois: ObjectInputStream) {
        val size = ois.readInt()
        this.data = mutableListOf()
        for (i in 0..size) {
            (this.data as MutableList).add(readData(ois))
        }
    }

    fun readData(ois: ObjectInputStream): T
}
