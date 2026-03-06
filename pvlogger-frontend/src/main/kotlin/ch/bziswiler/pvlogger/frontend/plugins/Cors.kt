package ch.bziswiler.pvlogger.frontend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCors() {
    install(CORS) {
        allowHost("localhost:4200", schemes = listOf("http"))
        allowHost("localhost:8085")
        allowHost("0.0.0.0:8085")
        allowHost("127.0.0.1:8085")
        allowHeader("X-Requested-With")
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.IfNoneMatch)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Upgrade)
        allowHeader(HttpHeaders.CacheControl)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        allowSameOrigin = true
    }
}
