package ch.bziswiler.pvlogger.fusionsolar.schema

import ch.bziswiler.pvlogger.fusionsolar.model.KpiStationExtendedData
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataType
import ch.bziswiler.pvlogger.fusionsolar.webservice.FusionSolarDataType.entries
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.update

object FsStationExtendedData : Table("fs_station_extended_data") {
    val collectTime = long("collect_time").index()
    val type = enumerationByName("type", entries.toTypedArray().maxOf { it.identifier.length }, FusionSolarDataType::class).index()
    override val primaryKey = PrimaryKey(collectTime, type)
    val stationCode = text("station_code")
    val installedCapacity = double("installed_capacity").nullable()
    val radiationIntensity = double("radiation_intensity").nullable()
    val theoryPower = double("theory_power").nullable()
    val performanceRatio = double("performance_ratio").nullable()
    val inverterPower = double("inverter_power").nullable()
    val ongridPower = double("ongrid_power").nullable()
    val usePower = double("use_power").nullable()
    val powerProfit = double("power_profit").nullable()
    val perpowerRatio = double("perpower_ratio").nullable()
    val reductionTotalCo2 = double("reduction_total_co2").nullable()
    val reductionTotalCoal = double("reduction_total_coal").nullable()
    val reductionTotalTree = double("reduction_total_tree").nullable()
}

fun FsStationExtendedData.update(data: KpiStationExtendedData, type: FusionSolarDataType) {
    FsStationExtendedData.update({ collectTime.eq(data.collectTime).and(FsStationExtendedData.type.eq(type)) }) {
        it[stationCode] = data.stationCode
        it[installedCapacity] = data.dataItemMap.installedCapacity
        it[radiationIntensity] = data.dataItemMap.radiationIntensity
        it[theoryPower] = data.dataItemMap.theoryPower
        it[performanceRatio] = data.dataItemMap.performanceRatio
        it[inverterPower] = data.dataItemMap.inverterPower
        it[ongridPower] = data.dataItemMap.ongridPower
        it[usePower] = data.dataItemMap.usePower
        it[powerProfit] = data.dataItemMap.powerProfit
        it[perpowerRatio] = data.dataItemMap.perpowerRatio
        it[reductionTotalCo2] = data.dataItemMap.reductionTotalCo2
        it[reductionTotalCoal] = data.dataItemMap.reductionTotalCoal
        it[reductionTotalTree] = data.dataItemMap.reductionTotalTree
    }
}

fun FsStationExtendedData.saveIgnore(data: KpiStationExtendedData, type: FusionSolarDataType) =
    FsStationExtendedData.insertIgnore {
        it[collectTime] = data.collectTime
        it[FsStationExtendedData.type] = type
        it[stationCode] = data.stationCode
        it[installedCapacity] = data.dataItemMap.installedCapacity
        it[radiationIntensity] = data.dataItemMap.radiationIntensity
        it[theoryPower] = data.dataItemMap.theoryPower
        it[performanceRatio] = data.dataItemMap.performanceRatio
        it[inverterPower] = data.dataItemMap.inverterPower
        it[ongridPower] = data.dataItemMap.ongridPower
        it[usePower] = data.dataItemMap.usePower
        it[powerProfit] = data.dataItemMap.powerProfit
        it[perpowerRatio] = data.dataItemMap.perpowerRatio
        it[reductionTotalCo2] = data.dataItemMap.reductionTotalCo2
        it[reductionTotalCoal] = data.dataItemMap.reductionTotalCoal
        it[reductionTotalTree] = data.dataItemMap.reductionTotalTree
    }
