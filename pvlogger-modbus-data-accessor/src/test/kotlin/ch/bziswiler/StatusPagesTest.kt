package ch.bziswiler

import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_KPI_VALUES
import ch.bziswiler.testutils.TestBase
import ch.bziswiler.testutils.TestDataFactory.cleanAllTables
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatusPagesTest : TestBase() {

    @Test
    fun `BadRequestException returns 400`() = withApp {
        startApplication()
        cleanAllTables()
        val response = client.post("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_STORE_KPI_VALUES") {
            contentType(ContentType.Application.Json)
            setBody("{invalid}")
        }
        assertEquals(BadRequest, response.status)
        assertEquals("Bad request", response.bodyAsText())
    }

    @Test
    fun `DateTimeParseException returns 400`() = withApp {
        startApplication()
        cleanAllTables()
        val response = client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH/31-13-2025")
        assertEquals(BadRequest, response.status)
        assertEquals("Invalid date format", response.bodyAsText())
    }

    @Test
    fun `unhandled exception returns 500`() = withApp {
        startApplication()
        cleanAllTables()
        val response = client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
            url { appendPathSegments("MLD", "1", "37113", "job-1") }
        }
        assertEquals(InternalServerError, response.status)
        assertEquals("Internal server error", response.bodyAsText())
    }
}
