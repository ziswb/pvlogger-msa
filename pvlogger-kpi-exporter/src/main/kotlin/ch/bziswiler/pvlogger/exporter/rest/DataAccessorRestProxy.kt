package ch.bziswiler.pvlogger.exporter.rest

import ch.bziswiler.pvlogger.exporter.configuration.ModbusDataAccessConfig
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_DATA_VERSION_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_KPI_PATH
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import ch.bziswiler.pvlogger.shared.rest.PvHttpClient.client
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.OK
import mu.KotlinLogging

class DataAccessorRestProxy(private val config: ModbusDataAccessConfig) {

    private val logger = KotlinLogging.logger {}

    suspend fun retrieveLatestKpis(): KpiValuesDto? = client.get {
        logger.info { "Getting latest KPIs" }
        url {
            host = config.host
            port = config.port
            pathSegments = listOf(DATA_ACCESSOR_READ_DATA_VERSION_PATH, DATA_ACCESSOR_READ_LATEST_KPI_PATH)
        }
    }.let {
        when (it.status) {
            OK -> {
                logger.info { "Got latest KPIs: '$it'" }
                it.body<KpiValuesDto>()
            }

            NoContent -> {
                logger.info { "No KPIs available: '$it'" }
                null
            }

            else -> {
                logger.warn { "Status: ${it.status}, no KPIs found." }
                null
            }
        }
    }
}
