package ch.bziswiler.pvlogger.frontend.service

import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto

/**
 * Where [LiveKpiPublisher] reads the latest KPI row from. Implemented by
 * `DataAccessorRestProxy`; tests supply a lambda.
 */
fun interface LatestKpiSource {
    /** Returns the latest KPI row, or `null` if the source currently has none. */
    suspend fun retrieveLatestKpis(): KpiValuesDto?
}
