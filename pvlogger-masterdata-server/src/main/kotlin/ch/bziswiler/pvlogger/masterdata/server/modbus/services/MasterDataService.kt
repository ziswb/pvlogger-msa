package ch.bziswiler.pvlogger.masterdata.server.modbus.services

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.masterdata.server.modbus.schema.MasterDataDao
import ch.bziswiler.pvlogger.masterdata.server.modbus.schema.MasterDataTable.category
import ch.bziswiler.pvlogger.masterdata.server.modbus.schema.MasterDataTable.deviceType
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and

class MasterDataService(private val dao: MasterDataDao) {
    fun registerDefinitionList(
        categories: List<Category>,
        deviceTypes: List<DeviceType>,
    ) = runBlocking {
        dao.registerDefinitionList(whereCondition(categories, deviceTypes))
    }

    private fun whereCondition(
        categories: List<Category>,
        deviceTypes: List<DeviceType>,
    ) = category.inList(categories).and(deviceType.inList(deviceTypes))
}
