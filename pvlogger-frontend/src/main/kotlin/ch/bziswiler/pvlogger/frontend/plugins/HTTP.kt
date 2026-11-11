package ch.bziswiler.pvlogger.frontend.plugins

import io.ktor.http.content.LastModifiedVersion
import io.ktor.http.content.versions
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.ResponseBodyReadyForSend
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

fun Application.configureHTTP() {
    install(XForwardedHeaders)
    // Must come before ConditionalHeaders, which turns the versions into headers and 304s.
    install(DropLastModified)
    // Answers If-None-Match with 304 for the ETags the static routes set (see Routing.kt).
    install(ConditionalHeaders)
    install(Compression) { gzip {} }
    install(DefaultHeaders) {}
}

/**
 * The fat JAR stores every file with the same fixed date (1980-02-01, for
 * reproducible archives), so Last-Modified never changes between releases and
 * a client that revalidates by date alone would keep an old index.html. The
 * ETag is the only validator worth sending.
 */
private val DropLastModified = createApplicationPlugin("DropLastModified") {
    on(ResponseBodyReadyForSend) { _, content ->
        if (content.versions.any { it is LastModifiedVersion }) {
            content.versions = content.versions.filterNot { it is LastModifiedVersion }
        }
    }
}
