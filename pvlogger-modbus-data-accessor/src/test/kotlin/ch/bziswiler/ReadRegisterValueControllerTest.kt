package ch.bziswiler

import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_KW_TODAY_SO_FAR_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.testutils.TestBase
import ch.bziswiler.testutils.TestDataFactory.cleanAllTables
import ch.bziswiler.testutils.TestDataFactory.seedIntegerValue
import ch.bziswiler.testutils.TestDataFactory.seedResponseRow
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.appendPathSegments
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ReadRegisterValueControllerTest : TestBase() {

    @Test
    fun `read latest I32 value with valid params`() = withApp {
        startApplication()
        cleanAllTables()
        seedResponseRow(id = 10, jobId = "job-A", logicDeviceId = 5u, responseTime = LocalDateTime.now())
        seedIntegerValue(responseId = 10, registerAddress = 37113, value = 42)

        val response = client.get("/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
            url { appendPathSegments("I32", "5", "37113", "job-A") }
        }
        assertEquals(OK, response.status)
        assertEquals("42", response.bodyAsText())
    }

    @Test
    fun `read latest returns 404 for wrong jobId`() = withApp {
        startApplication()
        cleanAllTables()
        seedResponseRow(id = 11, jobId = "job-A", logicDeviceId = 5u, responseTime = LocalDateTime.now())
        seedIntegerValue(responseId = 11, registerAddress = 37113, value = 42)

        val response = client.get("/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
            url { appendPathSegments("I32", "5", "37113", "job-B") }
        }
        assertEquals(NotFound, response.status)
    }

    @Test
    fun `read latest with missing path params returns 404`() = withApp {
        startApplication()
        cleanAllTables()
        val response = client.get("/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
            url { appendPathSegments("I32", "5") }
        }
        assertEquals(NotFound, response.status)
    }

    @Test
    fun `kWh today computes day-over-day difference`() = withApp {
        startApplication()
        cleanAllTables()
        val yesterday = LocalDateTime.now().minusDays(1)
        val today = LocalDateTime.now()

        seedResponseRow(id = 20, jobId = "energy", responseTime = yesterday)
        seedIntegerValue(responseId = 20, registerAddress = 32080, value = 100)

        seedResponseRow(id = 21, jobId = "energy", responseTime = today)
        seedIntegerValue(responseId = 21, registerAddress = 32080, value = 150)

        val response = client.get("/$DATA_ACCESSOR_READ_KW_TODAY_SO_FAR_PATH") {
            url { appendPathSegments("I32", "32080") }
        }
        assertEquals(OK, response.status)
        assertEquals("50", response.bodyAsText())
    }
}
