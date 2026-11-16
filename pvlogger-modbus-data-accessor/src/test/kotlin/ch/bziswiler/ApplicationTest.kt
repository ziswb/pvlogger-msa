package ch.bziswiler

import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.ktor.HEALTH_LIVE_PATH
import ch.bziswiler.pvlogger.shared.ktor.HEALTH_READY_PATH
import ch.bziswiler.testutils.TestBase
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ApplicationTest : TestBase() {
    @Test
    fun testRoot() = withApp {
        client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
            url {
                appendPathSegments("I32", "16", "37113")
            }
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
            assertTrue(bodyAsText().isEmpty())
        }
    }

    // Backlog 60: the probes the chart uses; ready means the pool reaches the database.
    @Test
    fun `health endpoints answer and readiness checks the database`() = withApp {
        assertEquals(HttpStatusCode.OK, client.get(HEALTH_LIVE_PATH).status)
        client.get(HEALTH_READY_PATH).apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("ok", bodyAsText())
        }
    }

    // Security finding 29: the pool is closed on shutdown, not left to the JVM exit.
    @Test
    fun `closes the database pool when the application stops`() {
        withApp {
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
                url { appendPathSegments("I32", "16", "37113") }
            }
            assertTrue(openConnectionsToTestDb() > 0, "the running application holds a pooled connection")
        }
        // A closed backend can take a moment to leave pg_stat_activity.
        val deadline = System.currentTimeMillis() + 5_000
        while (openConnectionsToTestDb() > 0 && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertEquals(0, openConnectionsToTestDb())
    }
}
