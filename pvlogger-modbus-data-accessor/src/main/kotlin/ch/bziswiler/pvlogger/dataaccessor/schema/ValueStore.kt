package ch.bziswiler.pvlogger.dataaccessor.schema

import ch.bziswiler.pvlogger.modbus.ModbusRegisterValue

interface ValueStore<in ModbusRegisterValue> {

    fun store(responseId: Int, registerValue: ModbusRegisterValue)
}