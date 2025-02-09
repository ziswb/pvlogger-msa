package ch.bziswiler.pvlogger.crepuscular.rest

import ch.bziswiler.pvlogger.crepuscular.service.SunriseSunsetService
import ch.bziswiler.pvlogger.shared.rest.SunriseSunsetAccessorPaths.SUNRISE_SUNSET_TODAY_PATH
import ch.bziswiler.pvlogger.shared.rest.SunriseSunsetAccessorPaths.SUNRISE_SUNSET_VERSION_PATH
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.sunriseSunsetToday(service: SunriseSunsetService) {
    get("/$SUNRISE_SUNSET_VERSION_PATH/${SUNRISE_SUNSET_TODAY_PATH}") {
        call.response.status(HttpStatusCode.OK)
        call.respond(service.sunriseSunsetToday())
    }
}