package ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema

import ch.bziswiler.fusionsolar.model.KpiStationExtendedData
import ch.bziswiler.pvlogger.business.model.huawei.fusionsolar.FusionSolarDataType
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ResponseToExtendedDataMapper {

    companion object {
        fun saveExtendedData(responseId: EntityID<Int>, data: List<KpiStationExtendedData>, type: FusionSolarDataType) {
            data.forEach { extendedData ->
                transaction {
                    FsStationExtendedData.saveIgnore(extendedData, type)
                    FsStationExtendedData.update(extendedData, type)
                    saveOrUpdateMapping(responseId, extendedData, type)
                }
            }
        }

        private fun saveOrUpdateMapping(responseId: EntityID<Int>, data: KpiStationExtendedData, type: FusionSolarDataType) {
            FsResponseToStationExtendedData
                .slice(FsResponseToStationExtendedData.responseId)
                .select { FsResponseToStationExtendedData.collectTime.eq(data.collectTime).and(FsResponseToStationExtendedData.type.eq(type)) }
                .toList().let { rows ->
                    if (rows.isEmpty()) {
                        insertData(responseId, data, type)
                    } else {
                        updateData(responseId, rows)
                    }
                }
        }

        private fun updateData(responseId: EntityID<Int>, rows: List<ResultRow>) {
            FsResponseToStationExtendedData.update({ FsResponseToStationExtendedData.responseId.eq(responseId.value) }) {
                it[FsResponseToStationExtendedData.responseId] = rows.first()[FsResponseToStationExtendedData.responseId]
            }
        }

        private fun insertData(responseId: EntityID<Int>, data: KpiStationExtendedData, type: FusionSolarDataType) {
            FsResponseToStationExtendedData.insert {
                it[FsResponseToStationExtendedData.responseId] = responseId.value
                it[collectTime] = data.collectTime
                it[FsResponseToStationExtendedData.type] = type
            }
        }
    }
}
