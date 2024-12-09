package ch.bziswiler.pvlogger.frontend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Application.configureRouting(registry: PrometheusMeterRegistry) {
    routing {
        staticResources("/", "static")
        get("/metrics") { call.respond(registry.scrape()) }
    }
}
