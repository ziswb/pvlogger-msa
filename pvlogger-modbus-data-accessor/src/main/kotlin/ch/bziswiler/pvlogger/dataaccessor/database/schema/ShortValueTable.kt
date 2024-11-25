package ch.bziswiler.pvlogger.dataaccessor.database.schema

import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterShortValue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object ShortValueTable : IntIdTable("modbus_register_short_values"), ValueStore<ModbusRegisterShortValue> {
    val response_id = integer("response_id").references(ResponseTable.id)
    val register_address = integer("register_address")
    val register_value = short("register_value").nullable()
    override fun store(responseId: Int, registerValue: ModbusRegisterShortValue) {
        ShortValueTable.insert {
            it[response_id] = responseId
            it[register_address] = registerValue.definition.registerAddress.value
            it[register_value] = registerValue.value
        }
    }
}
