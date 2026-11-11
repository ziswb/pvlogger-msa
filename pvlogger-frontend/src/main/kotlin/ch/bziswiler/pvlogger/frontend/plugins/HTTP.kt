package ch.bziswiler.pvlogger.frontend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

fun Application.configureHTTP() {
    install(XForwardedHeaders)
    // Answers If-None-Match with 304 for the ETags the static routes set (see Routing.kt).
    install(ConditionalHeaders)
    install(Compression) { gzip {} }
    install(DefaultHeaders) {}
}
