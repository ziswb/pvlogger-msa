package ch.bziswiler.pvlogger.frontend.rest

import ch.bziswiler.pvlogger.frontend.configuration.DataAccessorConfig
import ch.bziswiler.pvlogger.frontend.service.LatestKpiSource
import ch.bziswiler.pvlogger.shared.ktor.PvHttpClient.client
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_KPI_PATH
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode.Companion.OK
import kotlin.time.Duration.Companion.seconds

class DataAccessorRestProxy(private val config: DataAccessorConfig) : LatestKpiSource {

    private val logger = KotlinLogging.logger {}

    override suspend fun retrieveLatestKpis(): KpiValuesDto? = client.get {
        logger.debug { "Getting latest KPIs" }
        url {
            host = config.host
            port = config.port
            pathSegments = listOf(DATA_ACCESSOR_API_V1_PATH, DATA_ACCESSOR_READ_LATEST_KPI_PATH)
        }
        // The shared client allows 10 min for the collectors' bulk writes; a
        // single latest-KPI read must fail long before the next poll is due.
        timeout { requestTimeoutMillis = REQUEST_TIMEOUT.inWholeMilliseconds }
    }.let {
        when (it.status) {
            OK -> {
                logger.debug { "Got latest KPIs: '$it'" }
                it.body<KpiValuesDto>()
            }

            else -> {
                logger.warn { "Status: ${it.status}, no KPIs found." }
                null
            }
        }
    }

    private companion object {
        val REQUEST_TIMEOUT = 15.seconds
    }
}
