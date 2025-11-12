package ch.bziswiler.pvlogger.exporter.service

import ch.bziswiler.pvlogger.exporter.rest.DataAccessorRestProxy
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class KpiExporter(
    private val dataAccessor: DataAccessorRestProxy,
    private val exportService: ExportService,
) {
    private val logger = KotlinLogging.logger {}

    fun exportKpis() {
        runBlocking {
            when (val data = dataAccessor.retrieveLatestKpis()) {
                null -> logger.warn { "KPIs to be exported is null!" }
                else -> exportService.export(data)
            }
        }
    }
}