package ch.bziswiler.pvlogger.dataaccessor.configuration

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import java.time.format.DateTimeParseException

private val logger = KotlinLogging.logger {}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            logger.warn(cause) { "Bad request" }
            call.respondText("Bad request", status = HttpStatusCode.BadRequest)
        }
        exception<DateTimeParseException> { call, cause ->
            logger.warn(cause) { "Invalid date format" }
            call.respondText("Invalid date format", status = HttpStatusCode.BadRequest)
        }
        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unhandled exception" }
            call.respondText("Internal server error", status = HttpStatusCode.InternalServerError)
        }
    }
}
