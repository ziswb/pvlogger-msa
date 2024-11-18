package ch.bziswiler.pvlogger.dataaccessor.database.schema

interface ValueStore<in ModbusRegisterValue> {

    fun store(responseId: Int, registerValue: ModbusRegisterValue)
}