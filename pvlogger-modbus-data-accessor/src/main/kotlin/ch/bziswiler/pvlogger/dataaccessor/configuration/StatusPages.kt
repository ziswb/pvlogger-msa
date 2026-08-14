package ch.bziswiler.pvlogger.dataaccessor.configuration

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import java.time.format.DateTimeParseException
import ch.bziswiler.pvlogger.shared.ktor.configureStatusPages as configureSharedStatusPages

private val logger = KotlinLogging.logger {}

fun Application.configureStatusPages() =
    configureSharedStatusPages {
        exception<DateTimeParseException> { call, cause ->
            logger.warn(cause) { "Invalid date format" }
            call.respondText("Invalid date format", status = HttpStatusCode.BadRequest)
        }
    }
