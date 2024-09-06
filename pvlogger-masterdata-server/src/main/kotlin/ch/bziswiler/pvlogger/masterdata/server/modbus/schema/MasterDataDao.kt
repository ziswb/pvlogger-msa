package ch.bziswiler.pvlogger.masterdata.server.modbus.schema

import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Quantity
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Scope
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class MasterDataDao {
    private fun <T> dbQuery(block: () -> T): T = transaction { block() }

    fun insertBatch(
        devType: DeviceType,
        registerDefinitions: List<ModbusRegisterDefinition>,
    ) {
        dbQuery {
            MasterDataTable.batchInsert(registerDefinitions, ignore = true, shouldReturnGeneratedValues = false) {
                this[MasterDataTable.deviceType] = devType
                this[MasterDataTable.no] = it.no
                this[MasterDataTable.name] = it.name
                this[MasterDataTable.readWrite] = it.readWrite
                this[MasterDataTable.type] = it.type
                this[MasterDataTable.unit] = it.unit
                this[MasterDataTable.gain] = it.gain
                this[MasterDataTable.address] = it.registerAddress.value
                this[MasterDataTable.quantity] = it.quantity.value
                this[MasterDataTable.scope] = it.scope.value
                this[MasterDataTable.category] = it.category
            }
        }
    }

    fun registerDefinitionList(whereCondition: Op<Boolean>) =
        dbQuery {
            MasterDataTable.selectAll().where { whereCondition }.orderBy(MasterDataTable.address to SortOrder.ASC).map {
                ModbusRegisterDefinition(
                    it[MasterDataTable.no],
                    it[MasterDataTable.name],
                    it[MasterDataTable.readWrite],
                    it[MasterDataTable.type],
                    it[MasterDataTable.unit],
                    it[MasterDataTable.gain],
                    RegisterAddress(it[MasterDataTable.address]),
                    Quantity(it[MasterDataTable.quantity]),
                    Scope(it[MasterDataTable.scope].orEmpty()),
                    it[MasterDataTable.category],
                )
            }
        }
}
