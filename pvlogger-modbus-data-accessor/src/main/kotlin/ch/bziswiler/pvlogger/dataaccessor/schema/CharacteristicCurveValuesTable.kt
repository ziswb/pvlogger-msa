package ch.bziswiler.pvlogger.dataaccessor.schema

import ch.bziswiler.pvlogger.modbus.ModbusRegisterShortArrayValue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object CharacteristicCurveValuesTable : IntIdTable("modbus_cc_data"),
    ValueStore<ModbusRegisterShortArrayValue> {
    private val response_id = integer("response_id").references(ResponseTable.id)
    private val register_address = integer("register_address")
    private val number_of_points = ushort("number_of_points")
    private val p_value_at_point_01 = ushort("p_value_at_point_01")
    private val c_value_at_point_01 = short("c_value_at_point_01")
    private val p_value_at_point_02 = ushort("p_value_at_point_02")
    private val c_value_at_point_02 = short("c_value_at_point_02")
    private val p_value_at_point_03 = ushort("p_value_at_point_03")
    private val c_value_at_point_03 = short("c_value_at_point_03")
    private val p_value_at_point_04 = ushort("p_value_at_point_04")
    private val c_value_at_point_04 = short("c_value_at_point_04")
    private val p_value_at_point_05 = ushort("p_value_at_point_05")
    private val c_value_at_point_05 = short("c_value_at_point_05")
    private val p_value_at_point_06 = ushort("p_value_at_point_06")
    private val c_value_at_point_06 = short("c_value_at_point_06")
    private val p_value_at_point_07 = ushort("p_value_at_point_07")
    private val c_value_at_point_07 = short("c_value_at_point_07")
    private val p_value_at_point_08 = ushort("p_value_at_point_08")
    private val c_value_at_point_08 = short("c_value_at_point_08")
    private val p_value_at_point_09 = ushort("p_value_at_point_09")
    private val c_value_at_point_09 = short("c_value_at_point_09")
    private val p_value_at_point_10 = ushort("p_value_at_point_10")
    private val c_value_at_point_10 = short("c_value_at_point_10")
    override fun store(responseId: Int, registerValue: ModbusRegisterShortArrayValue) {
        if (registerValue.value.size == 21) {
            CharacteristicCurveValuesTable.insert {
                it[response_id] = responseId
                it[register_address] = registerValue.definition.registerAddress.value
                it[number_of_points] = registerValue.value[0].toUShort()
                it[p_value_at_point_01] = registerValue.value[1].toUShort()
                it[c_value_at_point_01] = registerValue.value[2]
                it[p_value_at_point_02] = registerValue.value[3].toUShort()
                it[c_value_at_point_02] = registerValue.value[4]
                it[p_value_at_point_03] = registerValue.value[5].toUShort()
                it[c_value_at_point_03] = registerValue.value[6]
                it[p_value_at_point_04] = registerValue.value[7].toUShort()
                it[c_value_at_point_04] = registerValue.value[8]
                it[p_value_at_point_05] = registerValue.value[9].toUShort()
                it[c_value_at_point_05] = registerValue.value[10]
                it[p_value_at_point_06] = registerValue.value[11].toUShort()
                it[c_value_at_point_06] = registerValue.value[12]
                it[p_value_at_point_07] = registerValue.value[13].toUShort()
                it[c_value_at_point_07] = registerValue.value[14]
                it[p_value_at_point_08] = registerValue.value[15].toUShort()
                it[c_value_at_point_08] = registerValue.value[16]
                it[p_value_at_point_09] = registerValue.value[17].toUShort()
                it[c_value_at_point_09] = registerValue.value[18]
                it[p_value_at_point_10] = registerValue.value[19].toUShort()
                it[c_value_at_point_10] = registerValue.value[20]
            }
        }
    }
}
