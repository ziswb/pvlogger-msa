package ch.bziswiler.pvlogger.dataaccessor.database.schema

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object HousePowerKpiTable : IntIdTable("house_power_kpi_values") {
    val dateCreated = datetime("date_created").defaultExpression(CurrentDateTime)
    val jobId = text("job_id").default("NO-JOB-ID-GIVEN")
    val solarPower = double("solar_power").default(0.0)
    val solarToHousePower = double("solar_to_house_power").default(0.0)
    val gridToHousePower = double("grid_to_house_power").default(0.0)
    val solarToGridPower = double("solar_to_grid_power").default(0.0)
    val ownConsumption = double("own_consumption").default(0.0)
    val dailyEnergyYield = double("daily_energy_yield").default(0.0)
    val dailyEnergySelfCovered = double("daily_energy_self_covered").default(0.0)
    val dailyEnergyExported = double("daily_energy_exported").default(0.0)
    val dailyEnergyImported = double("daily_energy_imported").default(0.0)
    val dailyEnergyConsumed = double("daily_energy_consumed").default(0.0)
}
