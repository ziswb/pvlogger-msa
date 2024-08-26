package ch.bziswiler.pvlogger.modbus.tcp.frame

import java.io.ObjectInputStream

class UShortData(override var data: List<UShort>) : Data<UShort> {
    override fun readData(ois: ObjectInputStream): UShort = ois.readUnsignedShort().toUShort()
}
