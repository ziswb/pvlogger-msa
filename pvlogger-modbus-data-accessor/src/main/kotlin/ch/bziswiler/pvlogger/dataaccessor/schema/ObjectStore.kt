package ch.bziswiler.pvlogger.dataaccessor.schema

import ch.bziswiler.pvlogger.modbus.ModbusObject


interface ObjectStore {
    fun store(responseId: Int, modbusObject: ModbusObject)
}