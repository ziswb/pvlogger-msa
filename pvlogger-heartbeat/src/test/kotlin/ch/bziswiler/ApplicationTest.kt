package ch.bziswiler

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions
import kotlin.test.Test

class ApplicationTest : BaseApplicationTest() {

    private val rootPath = "/"

    @Test
    fun testRoot() =
        withApp {
            client.get(rootPath).apply {
                Assertions.assertEquals(HttpStatusCode.NotFound, status)
            }
        }
}
