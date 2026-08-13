package ch.bziswiler.pvlogger.dataaccessor.database.schema

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

object RawKpiRegisterValuesTable : IntIdTable("raw_kpi_register_values") {
    val dateCreated = timestamp("date_created").defaultExpression(CurrentTimestamp)
    val jobId = text("job_id").default("NO-JOB-ID-GIVEN")
    val inverterActivePower = double("inverter_active_power").default(0.0)
    val meterActivePower = double("meter_active_power").default(0.0)
    val inverterDailyYield = double("inverter_daily_yield").default(0.0)
    val inverterAccumulatedYield = double("inverter_accumulated_yield").default(0.0)
    val meterExportedEnergy = double("meter_exported_energy").default(0.0)
    val meterImportedEnergy = double("meter_imported_energy").default(0.0)
}
