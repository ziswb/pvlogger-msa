package ch.bziswiler.pvlogger.sunrise.rest

import ch.bziswiler.pvlogger.sunrise.service.SunriseSunsetService
import ch.bziswiler.pvlogger.sunrise.rest.SunriseSunsetAccessorPaths.SUNRISE_SUNSET_TODAY_PATH
import ch.bziswiler.pvlogger.sunrise.rest.SunriseSunsetAccessorPaths.SUNRISE_SUNSET_VERSION_PATH
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.sunriseSunsetToday(service: SunriseSunsetService) {
    get("/$SUNRISE_SUNSET_VERSION_PATH/${SUNRISE_SUNSET_TODAY_PATH}") {
        call.respond(HttpStatusCode.OK, service.sunriseSunsetToday())
    }
}