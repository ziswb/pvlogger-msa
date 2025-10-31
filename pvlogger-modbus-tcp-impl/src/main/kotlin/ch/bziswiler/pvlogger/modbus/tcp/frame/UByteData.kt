package ch.bziswiler.pvlogger.modbus.tcp.frame

import java.io.ObjectInputStream

class UByteData(
    override var data: List<UByte>,
) : Data<UByte> {
    override fun readData(ois: ObjectInputStream): UByte = ois.readUnsignedByte().toUByte()
}
