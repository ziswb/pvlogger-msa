package ch.bziswiler.pvlogger.dataaccessor.database.access

import kotlinx.serialization.Serializable

@Serializable
data class RawKpiRegisterValues(
    val jobId: String = "NO-JOB-ID-GIVEN",
    val inverterActivePower: Double = 0.0,
    val meterActivePower: Double = 0.0,
    val inverterDailyYield: Double = 0.0,
    val inverterAccumulatedYield: Double = 0.0,
    val meterExportedEnergy: Double = 0.0,
    val meterImportedEnergy: Double = 0.0,
)
