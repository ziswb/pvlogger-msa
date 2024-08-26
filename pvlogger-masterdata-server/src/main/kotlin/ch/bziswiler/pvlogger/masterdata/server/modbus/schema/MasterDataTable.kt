package ch.bziswiler.pvlogger.masterdata.server.modbus.schema

import ch.bziswiler.pvlogger.masterdata.modbus.Category
import ch.bziswiler.pvlogger.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.masterdata.modbus.Gain
import ch.bziswiler.pvlogger.masterdata.modbus.ReadWrite
import ch.bziswiler.pvlogger.masterdata.modbus.Type
import ch.bziswiler.pvlogger.masterdata.modbus.Unit
import kotlin.enums.EnumEntries
import kotlin.reflect.KClass
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object MasterDataTable : IntIdTable("modbus_register_master_data") {
    val deviceType = enumByName("device_type", DeviceType.entries, DeviceType::class)
    val no = integer("no")
    val name = text("name")
    val readWrite = enumByName("read_write", ReadWrite.entries, ReadWrite::class)
    val type = enumerationByName("type", Type.entries.toTypedArray().maxOf { it.name.length }, Type::class)
    val unit = enumerationByName("unit", Unit.entries.toTypedArray().maxOf { it.name.length }, Unit::class)
    val gain = enumerationByName("gain", Gain.entries.toTypedArray().maxOf { it.name.length }, Gain::class)
    val address = integer("address").uniqueIndex()
    val quantity = integer("quantity")
    val scope = text("scope").nullable()
    val category = enumByName("category", Category.entries, Category::class)

    private inline fun <reified T : Enum<T>> enumByName(
        enumName: String,
        enumEntries: EnumEntries<T>,
        enumClass: KClass<T>,
    ): Column<T> = enumerationByName(enumName, enumEntries.toTypedArray().maxOf { it.name.length }, enumClass)
}
