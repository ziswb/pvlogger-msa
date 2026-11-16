package ch.bziswiler.pvlogger.frontend.plugins

import ch.bziswiler.pvlogger.shared.ktor.HEALTH_LIVE_PATH
import ch.bziswiler.pvlogger.shared.ktor.HEALTH_READY_PATH
import ch.bziswiler.pvlogger.shared.ktor.configureHealth
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Backlog item 47: hashed bundles are cached for good, everything else is revalidated. */
class SpaCachingTest {

    private fun spaTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            configureHTTP() // the real plugins, gzip included, as browsers ask for it
            routing { spaResources("spa-fixture") }
        }
        block()
    }

    @Test
    fun `the health probes are not swallowed by the SPA fallback`() = testApplication {
        application {
            configureHTTP()
            configureHealth()
            routing { spaResources("spa-fixture") }
        }
        for (path in listOf(HEALTH_LIVE_PATH, HEALTH_READY_PATH)) {
            val response = client.get(path)
            assertEquals(HttpStatusCode.OK, response.status, path)
            assertEquals("ok", response.bodyAsText(), path)
        }
    }

    @Test
    fun `index html is revalidated on every load`() = spaTest {
        for (path in listOf("/", "/index.html")) {
            val response = client.get(path)
            assertEquals(HttpStatusCode.OK, response.status, path)
            assertEquals("no-cache", response.headers[HttpHeaders.CacheControl], path)
            assertNotNull(response.headers[HttpHeaders.ETag], path)
        }
    }

    @Test
    fun `an unchanged index html answers 304`() = spaTest {
        val first = client.get("/") { header(HttpHeaders.AcceptEncoding, "gzip") }
        assertEquals("gzip", first.headers[HttpHeaders.ContentEncoding]) // the fixture is big enough to compress
        val etag = first.headers[HttpHeaders.ETag]!!

        val response = client.get("/") {
            header(HttpHeaders.AcceptEncoding, "gzip")
            header(HttpHeaders.IfNoneMatch, etag)
        }

        assertEquals(HttpStatusCode.NotModified, response.status)
    }

    @Test
    fun `hashed bundles are cached for a year`() = spaTest {
        for (path in listOf("/main-BHBFA2FT.js", "/styles-APNCLLVC.css")) {
            val response = client.get(path)
            assertEquals(HttpStatusCode.OK, response.status, path)
            assertEquals("public, max-age=31536000, immutable", response.headers[HttpHeaders.CacheControl], path)
        }
    }

    @Test
    fun `unhashed files are revalidated`() = spaTest {
        assertEquals("no-cache", client.get("/favicon.ico").headers[HttpHeaders.CacheControl])
    }

    @Test
    fun `only Angular's hash pattern counts as hashed`() {
        assertEquals("public, max-age=31536000, immutable", cacheControlFor("chunk-OSQMNGTH.js"))
        assertEquals("public, max-age=31536000, immutable", cacheControlFor("roboto-ABCD1234.woff2"))
        assertEquals("no-cache", cacheControlFor("index.html"))
        assertEquals("no-cache", cacheControlFor("3rdpartylicenses.txt"))
        assertEquals("no-cache", cacheControlFor("my-component.js")) // a word, not a hash
    }

    @Test
    fun `HEAD answers like GET without a body`() = spaTest {
        // Regression: the static route answered only GET, so `curl -I` got a 404.
        for (path in listOf("/", "/main-BHBFA2FT.js")) {
            val get = client.get(path)
            val head = client.head(path)
            assertEquals(HttpStatusCode.OK, head.status, path)
            assertEquals(get.headers[HttpHeaders.CacheControl], head.headers[HttpHeaders.CacheControl], path)
            assertEquals(get.headers[HttpHeaders.ETag], head.headers[HttpHeaders.ETag], path)
            assertTrue(head.bodyAsText().isEmpty(), path)
        }
    }

    @Test
    fun `no Last-Modified, so a date-only check cannot keep an old release`() = spaTest {
        // The fat JAR dates every file 1980-02-01: a client revalidating by date
        // alone got 304 for a changed index.html.
        val response = client.get("/") { header(HttpHeaders.AcceptEncoding, "gzip") }
        assertNull(response.headers[HttpHeaders.LastModified])
        assertNotNull(response.headers[HttpHeaders.ETag])

        val dateOnly = client.get("/") { header(HttpHeaders.IfModifiedSince, "Wed, 11 Nov 2099 08:00:00 GMT") }
        assertEquals(HttpStatusCode.OK, dateOnly.status)
    }
}
