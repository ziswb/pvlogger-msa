package ch.bziswiler

import io.ktor.server.testing.testApplication // Ktor server testing (marked)
import kotlin.test.Test

class ApplicationTest {

    @Test
    fun testRoot() =
        testApplication {
            application {
            }
        }
}
