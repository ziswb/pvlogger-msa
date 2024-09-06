package ch.bziswiler.pvlogger.dataaccessor.schema

import ch.bziswiler.pvlogger.shared.modbus.ModbusObject


interface ObjectStore {
    fun store(responseId: Int, modbusObject: ModbusObject)
}