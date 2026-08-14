package ch.bziswiler.pvlogger.shared.ktor

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.format.DateTimeParseException

class StatusPagesTest {

    private fun ApplicationTestBuilder.setupThrowingRoutes() {
        application {
            configureStatusPages {
                exception<DateTimeParseException> { call, _ ->
                    call.respondText("Invalid date format", status = HttpStatusCode.BadRequest)
                }
            }
            routing {
                get("/bad-request") { throw BadRequestException("boom") }
                get("/unhandled") { throw IllegalStateException("boom") }
                get("/extra") { throw DateTimeParseException("boom", "x", 0) }
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

    @Test
    fun `extension hook adds service-specific handlers`() = testApplication {
        setupThrowingRoutes()

        val response = client.get("/extra")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid date format", response.bodyAsText())
    }
}
