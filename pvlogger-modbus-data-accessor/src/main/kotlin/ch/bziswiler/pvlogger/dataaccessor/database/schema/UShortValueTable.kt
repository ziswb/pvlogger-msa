package ch.bziswiler.pvlogger.dataaccessor.database.schema

import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterUShortValue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object UShortValueTable : IntIdTable("modbus_register_ushort_values"), ValueStore<ModbusRegisterUShortValue> {
    private val response_id = integer("response_id").references(ResponseTable.id)
    private val register_address = integer("register_address")
    private val register_value = uinteger("register_value").nullable()
    override fun store(responseId: Int, registerValue: ModbusRegisterUShortValue) {
        UShortValueTable.insert {
            it[response_id] = responseId
            it[register_address] = registerValue.definition.registerAddress.value
            it[register_value] = registerValue.value.toUInt()
        }

    }
}
