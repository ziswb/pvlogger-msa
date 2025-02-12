package ch.bziswiler.pvlogger.fusionsolar.schema

import ch.bziswiler.pvlogger.fusionsolar.model.StationRealKpiData
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataType
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataType.STATION_REAL_KPI
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataType.entries
import ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema.FsResponse
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object FsStationRealKpi : IntIdTable("fs_station_real_kpi") {
    val response = integer("response").references(FsResponse.id)
    val type = enumerationByName("type", entries.toTypedArray().maxOf { it.identifier.length }, FusionSolarDataType::class).default(STATION_REAL_KPI)
    val stationCode = text("station_code").nullable()
    val dayPower = double("day_power")
    val monthPower = double("month_power")
    val totalPower = double("total_power")
    val dayIncome = double("day_income")
    val totalIncome = double("total_income")
    val realHealthState = integer("real_health_state")
}

fun FsStationRealKpi.save(responseId: EntityID<Int>, stationKpi: StationRealKpiData) =
    FsStationRealKpi.insert {
        it[response] = responseId.value
        it[stationCode] = stationKpi.stationCode
        it[dayPower] = stationKpi.dataItemMap.dayPower
        it[monthPower] = stationKpi.dataItemMap.monthPower
        it[totalPower] = stationKpi.dataItemMap.totalPower
        it[dayIncome] = stationKpi.dataItemMap.dayIncome
        it[totalIncome] = stationKpi.dataItemMap.totalIncome
        it[realHealthState] = stationKpi.dataItemMap.realHealthState
    }
