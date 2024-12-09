package ch.bziswiler.pvlogger.frontend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCors() {
    install(CORS) {
        allowHost("localhost:4200", schemes = listOf("http"))
        allowHeader(HttpHeaders.ContentType)
    }
}
