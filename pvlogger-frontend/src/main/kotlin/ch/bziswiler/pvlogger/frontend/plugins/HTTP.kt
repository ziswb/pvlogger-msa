package ch.bziswiler.pvlogger.frontend.plugins

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.CachingOptions
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import java.time.ZonedDateTime.now

fun Application.configureHTTP() {
    install(XForwardedHeaders)
    install(CachingHeaders) {
        options { _, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.CSS -> CachingOptions(expires = now().plusDays(1))
                else -> null
            }
        }
    }
    install(Compression) { gzip {} }
    install(DefaultHeaders) {}
}
