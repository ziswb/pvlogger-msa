package ch.bziswiler.pvlogger.heartbeat.rest

import ch.bziswiler.pvlogger.heartbeat.configuration.SunriseSunsetServiceConfig
import ch.bziswiler.pvlogger.shared.rest.PvHttpClient.client
import ch.bziswiler.pvlogger.shared.rest.SunriseSunsetAccessorPaths.SUNRISE_SUNSET_TODAY_PATH
import ch.bziswiler.pvlogger.shared.rest.SunriseSunsetAccessorPaths.SUNRISE_SUNSET_VERSION_PATH
import ch.bziswiler.pvlogger.shared.rest.SunriseSunsetPair
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode.Companion.OK
import mu.KotlinLogging

class SunriseSunsetRestProxy(private val config: SunriseSunsetServiceConfig) {

    private val logger = KotlinLogging.logger {}

    suspend fun retrieveSunriseSunset(): SunriseSunsetPair? = client.get {
        logger.info { "Getting latest KPIs" }
        url {
            host = config.host
            port = config.port
            pathSegments = listOf(SUNRISE_SUNSET_VERSION_PATH, SUNRISE_SUNSET_TODAY_PATH)
        }
    }.let {
        when (it.status) {
            OK -> {
                logger.info { "Got sunrise-sunset: '$it'" }
                it.body<SunriseSunsetPair>()
            }

            else -> {
                logger.warn { "Status: ${it.status}, no sunrise-sunset found." }
                null
            }
        }
    }
}