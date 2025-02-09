package ch.bziswiler.pvlogger.crepuscular.configuration

import ch.bziswiler.pvlogger.crepuscular.rest.sunriseSunsetToday
import ch.bziswiler.pvlogger.crepuscular.service.SunriseSunsetService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(service: SunriseSunsetService) {
    routing {
        sunriseSunsetToday(service)
    }
}
