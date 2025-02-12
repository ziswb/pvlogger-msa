package ch.bziswiler.pvlogger.sunrise.configuration

import ch.bziswiler.pvlogger.sunrise.rest.sunriseSunsetToday
import ch.bziswiler.pvlogger.sunrise.service.SunriseSunsetService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(service: SunriseSunsetService) {
    routing {
        sunriseSunsetToday(service)
    }
}
