package ch.bziswiler.pvlogger.dataaccessor.database.schema

import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterUIntValue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object UIntegerValueTable : IntIdTable("modbus_register_uinteger_values"), ValueStore<ModbusRegisterUIntValue> {
    val response_id = integer("response_id").references(ResponseTable.id)
    val register_address = integer("register_address")
    val register_value = ulong("register_value").nullable()
    override fun store(responseId: Int, registerValue: ModbusRegisterUIntValue) {
        UIntegerValueTable.insert {
            it[response_id] = responseId
            it[register_address] = registerValue.definition.registerAddress.value
            it[register_value] = registerValue.value.toULong()
        }
    }
}
