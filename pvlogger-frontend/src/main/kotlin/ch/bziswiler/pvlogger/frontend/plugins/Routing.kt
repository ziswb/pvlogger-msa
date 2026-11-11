package ch.bziswiler.pvlogger.frontend.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.http.content.ETagProvider
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.header
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        spaResources("static")
    }
}

/**
 * Serves the Angular build with the usual SPA caching (backlog item 47):
 * files with a content hash in their name never change, so browsers keep them
 * for a year without asking; everything else, above all index.html, is
 * revalidated on every load, so a new release shows up on the next reload.
 */
fun Route.spaResources(basePackage: String) {
    staticResources("/", basePackage) {
        etag(ETagProvider.StrongSha256)
        modify { resource, call ->
            val fileName = resource.path.substringAfterLast('/')
            call.response.header(HttpHeaders.CacheControl, cacheControlFor(fileName))
        }
    }
}

/** Angular's `outputHashing: all` names: `main-BHBFA2FT.js`, `styles-APNCLLVC.css`, `media/x-ABCD1234.woff2`. */
private val HASHED_FILE_NAME = Regex("""-[A-Z0-9]{8}\.[a-z0-9]+$""")

internal fun cacheControlFor(fileName: String): String =
    if (HASHED_FILE_NAME.containsMatchIn(fileName)) "public, max-age=31536000, immutable" else "no-cache"
