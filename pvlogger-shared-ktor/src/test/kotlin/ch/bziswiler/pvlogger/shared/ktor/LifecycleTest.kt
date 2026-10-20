package ch.bziswiler.pvlogger.shared.ktor

import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class LifecycleTest {
    private class CountingResource : AutoCloseable {
        val closes = AtomicInteger(0)

        override fun close() {
            closes.incrementAndGet()
        }
    }

    @Test
    fun `closes the resource once when the application stops`() {
        val resource = CountingResource()
        testApplication {
            application { closeOnShutdown(resource) }
            startApplication()
            assertEquals(0, resource.closes.get(), "must stay open while the application runs")
        }
        assertEquals(1, resource.closes.get())
    }

    @Test
    fun `a failing close does not stop the others from closing`() {
        val failing = AutoCloseable { throw IllegalStateException("boom") }
        val resource = CountingResource()
        testApplication {
            application {
                closeOnShutdown(failing)
                closeOnShutdown(resource)
            }
            startApplication()
        }
        assertEquals(1, resource.closes.get())
    }
}
