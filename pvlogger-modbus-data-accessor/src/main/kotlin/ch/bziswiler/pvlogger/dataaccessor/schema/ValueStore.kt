package ch.bziswiler.pvlogger.dataaccessor.schema

interface ValueStore<in ModbusRegisterValue> {

    fun store(responseId: Int, registerValue: ModbusRegisterValue)
}