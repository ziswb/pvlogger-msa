package ch.bziswiler.pvlogger.frontend.plugins

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatusPagesTest {

    private fun io.ktor.server.testing.ApplicationTestBuilder.setupThrowingRoutes() {
        application {
            configureStatusPages()
            routing {
                get("/bad-request") { throw BadRequestException("boom") }
                get("/unhandled") { throw IllegalStateException("boom") }
            }
        }
    }

    @Test
    fun `BadRequestException responds 400 without leaking the cause`() = testApplication {
        setupThrowingRoutes()

        val response = client.get("/bad-request")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Bad request", response.bodyAsText())
    }

    @Test
    fun `unhandled exception responds 500 without leaking the cause`() = testApplication {
        setupThrowingRoutes()

        val response = client.get("/unhandled")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Internal server error", response.bodyAsText())
    }
}
