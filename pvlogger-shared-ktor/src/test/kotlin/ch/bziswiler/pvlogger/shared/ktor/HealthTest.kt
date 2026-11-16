package ch.bziswiler.pvlogger.shared.ktor

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.awaitCancellation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class HealthTest {
    @Test
    fun `live answers without any check`() =
        testApplication {
            application { configureHealth(mapOf("db" to ReadinessCheck { error("down") })) }

            val response = client.get(HEALTH_LIVE_PATH)

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `ready passes when every check passes`() =
        testApplication {
            application { configureHealth(mapOf("db" to ReadinessCheck { })) }

            assertEquals(HttpStatusCode.OK, client.get(HEALTH_READY_PATH).status)
        }

    @Test
    fun `ready fails with only the names of the failed checks`() =
        testApplication {
            application {
                configureHealth(
                    mapOf(
                        "db" to ReadinessCheck { error("password=secret") },
                        "other" to ReadinessCheck { },
                    ),
                )
            }

            val response = client.get(HEALTH_READY_PATH)

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals("not ready: db", response.bodyAsText())
        }

    @Test
    fun `a hanging check fails after the timeout`() =
        testApplication {
            application {
                configureHealth(mapOf("slow" to ReadinessCheck { awaitCancellation() }), timeout = 50.milliseconds)
            }

            val response = client.get(HEALTH_READY_PATH)

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals("not ready: slow", response.bodyAsText())
        }
}
