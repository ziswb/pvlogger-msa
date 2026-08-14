package ch.bziswiler.pvlogger.shared.ktor

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MonitoringTest {

    @Test
    fun `metrics endpoint scrapes the prometheus registry`() = testApplication {
        application {
            configureMonitoring()
        }

        val response = client.get("/metrics")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("jvm_memory_used_bytes")) {
            "expected JVM metrics in the scrape output"
        }
    }
}
