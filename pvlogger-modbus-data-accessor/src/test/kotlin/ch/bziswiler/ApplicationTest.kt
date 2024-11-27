package ch.bziswiler

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ApplicationTest : BaseApplicationTest() {

    @Test
    fun testRoot() = withApp {
        client.get("/read-latest-register-value") {
            url {
                appendPathSegments("I32", "16", "37113")
            }
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
            assertTrue(bodyAsText().isEmpty())
        }
    }

    @Test
    fun `test so far today energy`() = withApp {
        client.get("/read-so-far-today-register-value") {
            url {
                appendPathSegments("I32", "16", "32080")
            }
        }.apply { assertEquals(HttpStatusCode.NotFound, status) }
    }
}
