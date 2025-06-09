package ch.bziswiler.pvlogger.dataaccessor.database.schema

import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusObject


interface ObjectStore {
    fun store(responseId: Int, modbusObject: ModbusObject)
}