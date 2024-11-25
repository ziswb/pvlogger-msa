package ch.bziswiler.pvlogger.dataaccessor.database.schema

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.stringify
import ch.bziswiler.pvlogger.shared.modbus.ModbusObject
import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterStringValue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object StringValuesTable : IntIdTable("modbus_register_string_values"), ValueStore<ModbusRegisterStringValue>,
    ObjectStore {
    val response_id = integer("response_id").references(ResponseTable.id)
    val register_address = integer("register_address")
    val register_value = text("register_value").nullable()
    override fun store(responseId: Int, registerValue: ModbusRegisterStringValue) {
        StringValuesTable.insert {
            it[response_id] = responseId
            it[register_address] = registerValue.definition.registerAddress.value
            it[register_value] = registerValue.value
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun store(responseId: Int, modbusObject: ModbusObject) {
        StringValuesTable.insert {
            it[response_id] = responseId
            it[register_address] = modbusObject.objectId.toInt()
            it[register_value] = modbusObject.objectValue.toByteArray().stringify()
        }
    }
}
