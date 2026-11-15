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
    /**
     * When the row was read, epoch milliseconds. Honoured only with the dev
     * setting DEV_ACCEPT_CLIENT_TIMESTAMPS (the history backfill, ADR-0025);
     * otherwise a row that carries it is refused (400). Without it the row is
     * dated on arrival.
     */
    val dateCreated: Long? = null,
)
