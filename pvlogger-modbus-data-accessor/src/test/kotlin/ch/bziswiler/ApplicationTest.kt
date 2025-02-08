package ch.bziswiler

import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_KW_TODAY_SO_FAR_PATH
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
        client.get("/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
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
        client.get("/$DATA_ACCESSOR_READ_KW_TODAY_SO_FAR_PATH") {
            url {
                appendPathSegments("I32", "32080")
            }
        }.apply { assertEquals(HttpStatusCode.NotFound, status) }
    }
}
