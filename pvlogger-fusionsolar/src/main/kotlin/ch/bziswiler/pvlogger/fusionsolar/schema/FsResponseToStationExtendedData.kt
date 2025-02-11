package ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema

import ch.bziswiler.pvlogger.business.model.huawei.fusionsolar.FusionSolarDataType
import ch.bziswiler.pvlogger.business.model.huawei.fusionsolar.FusionSolarDataType.entries
import org.jetbrains.exposed.sql.Table

object FsResponseToStationExtendedData : Table("fs_response_to_station_extended_data") {
    val responseId = integer("response_id").references(FsResponse.id)
    val collectTime = long("collect_time")
    val type = enumerationByName("type", entries.toTypedArray().maxOf { it.identifier.length }, FusionSolarDataType::class)
    override val primaryKey = PrimaryKey(responseId, collectTime, type)
}

object FsResponseToStationExtendedDataForeignKeyName {
    const val FK_NAME = "fk_response_to_station_extended_data"
}
