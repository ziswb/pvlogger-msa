package ch.bziswiler.pvlogger.dataaccessor.database.schema

import ch.bziswiler.pvlogger.shared.modbus.ModbusObject
import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterIntValue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object IntegerValueTable : IntIdTable("modbus_register_integer_values"), ValueStore<ModbusRegisterIntValue>,
    ObjectStore {
    val response_id = integer("response_id").references(ResponseTable.id)
    val register_address = integer("register_address")
    val register_value = integer("register_value").nullable()
    override fun store(responseId: Int, registerValue: ModbusRegisterIntValue) {
        IntegerValueTable.insert {
            it[response_id] = responseId
            it[register_address] = registerValue.definition.registerAddress.value
            it[register_value] = registerValue.value
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun store(responseId: Int, modbusObject: ModbusObject) {
        IntegerValueTable.insert {
            it[response_id] = responseId
            it[register_address] = modbusObject.objectId.toInt()
            it[register_value] = modbusObject.objectValue.toInt()
        }
    }
}
