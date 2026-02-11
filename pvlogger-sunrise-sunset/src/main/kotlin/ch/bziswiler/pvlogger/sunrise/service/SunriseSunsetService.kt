package ch.bziswiler.pvlogger.sunrise.service

import ch.bziswiler.pvlogger.sunrise.configuration.SunriseSunsetServiceConfig
import ch.bziswiler.pvlogger.sunrise.database.access.SunriseSunsetDao
import ch.bziswiler.pvlogger.sunrise.database.schema.SunriseSunsetData
import ch.bziswiler.pvlogger.sunrise.rest.SunriseSunsetPair
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.LocalDate.now
import java.time.LocalTime.of
import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.time.ZonedDateTime.of
import java.time.ZonedDateTime.parse
import java.time.format.DateTimeFormatter.ISO_DATE

class SunriseSunsetService(private val config: SunriseSunsetServiceConfig, private val dao: SunriseSunsetDao) {

    private val logger = KotlinLogging.logger {}
    private val notFormatted = "0"

    private val httpClient = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.INFO
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                allowSpecialFloatingPointValues = true
            })
        }
    }

    suspend fun sunriseSunsetToday() = sunriseSunsetPair(today())

    private suspend fun sunriseSunsetPair(date: String) = runCatching {
        logger.debug { "Starting sunriseSunsetRequest for date '$date'" }
        sunriseSunsetData(date).let { data ->
            logger.debug { "SunriseSunset result: $data" }
            dao.save(data)
            SunriseSunsetPair(toUtcDateTime(data.results.sunrise), toUtcDateTime(data.results.sunset))
        }
    }.onFailure { error: Throwable ->
        logger.error(error) { "solar-api failed" }
    }.recover { error: Throwable ->
        logger.error(error) { "recovering from solar-api failure - returning default values of today 5 AM to 10 PM" }
        SunriseSunsetPair(of(now(), of(5, 0), systemDefault()), of(now(), of(22, 0), systemDefault()))
    }.getOrThrow()

    private fun toUtcDateTime(dateTime: String): ZonedDateTime = zonedDateTimeZhr(zonedDateTimeUtc(dateTime))

    private suspend fun sunriseSunsetData(date: String) = httpClient.get { url(date) }.body<SunriseSunsetData>()

    private fun HttpRequestBuilder.url(date: String) {
        url {
            protocol = URLProtocol.HTTPS
            host = config.host
            port = config.port
            path(config.path)
            parameters.append("lat", config.latitude)
            parameters.append("lng", config.longitude)
            parameters.append("formatted", notFormatted)
            parameters.append("date", date)
        }
    }

    private fun zonedDateTimeZhr(sunriseUtc: ZonedDateTime) = sunriseUtc.withZoneSameInstant(systemDefault())

    private fun zonedDateTimeUtc(sunrise: String): ZonedDateTime = parse(sunrise).withZoneSameInstant(UTC)

    private fun today() = now().format(ISO_DATE)
}
