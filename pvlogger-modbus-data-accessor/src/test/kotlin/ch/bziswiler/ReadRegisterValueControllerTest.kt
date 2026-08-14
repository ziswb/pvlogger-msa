package ch.bziswiler

import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_MODBUS_VALUES
import ch.bziswiler.testutils.TestBase
import ch.bziswiler.testutils.TestDataFactory.cleanAllTables
import ch.bziswiler.testutils.TestDataFactory.seedIntegerValue
import ch.bziswiler.testutils.TestDataFactory.seedResponseRow
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class ReadRegisterValueControllerTest : TestBase() {

    @Test
    fun `read latest I32 value with valid params`() = withApp {
        startApplication()
        cleanAllTables()
        seedResponseRow(id = 10, jobId = "job-A", logicDeviceId = 5u, responseTime = Instant.now())
        seedIntegerValue(responseId = 10, registerAddress = 37113, value = 42)

        val response = client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
            url { appendPathSegments("I32", "5", "37113", "job-A") }
        }
        assertEquals(OK, response.status)
        assertEquals("42", response.bodyAsText())
    }

    @Test
    fun `read latest returns 404 for wrong jobId`() = withApp {
        startApplication()
        cleanAllTables()
        seedResponseRow(id = 11, jobId = "job-A", logicDeviceId = 5u, responseTime = Instant.now())
        seedIntegerValue(responseId = 11, registerAddress = 37113, value = 42)

        val response = client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
            url { appendPathSegments("I32", "5", "37113", "job-B") }
        }
        assertEquals(NotFound, response.status)
    }

    @Test
    fun `read latest with missing path params returns 404`() = withApp {
        startApplication()
        cleanAllTables()
        val response = client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
            url { appendPathSegments("I32", "5") }
        }
        assertEquals(NotFound, response.status)
    }

    @Test
    fun `fast collector dongle block round-trips through store and per-jobId read`() = withApp {
        startApplication()
        cleanAllTables()
        // Wire-contract pin for the single KPI aggregation path (ADR-0016):
        // this body is the byte-exact output of the Go fast collector's
        // dongle-block POST (shared registers definitions, reserved filler
        // already filtered, real payload captured 2026-08-14 while
        // exporting). The shared aggregator then reads 37516/37500/37502
        // back per jobId exactly like the GETs below.
        val goWireBody = """
            {"jobId":{"id":"fast-dongle-job"},"requestId":{"id":1},"requestTime":"2026-08-14T10:23:00.123456+02:00","responseTime":"2026-08-14T10:23:00.123456+02:00","header":{"protocolIdentifier":0,"protocolType":0,"dataLength":43,"logicDeviceId":100},"functionCode":{"value":3},"numberOfBytes":40,"registerValues":[{"definition":{"no":37498,"name":"Total input power","readWrite":"RO","type":"U32","unit":"W","gain":"ONE","registerAddress":{"value":37498},"quantity":{"value":2},"scope":{"value":""},"category":"DYNAMIC"},"value":6974},{"definition":{"no":37500,"name":"Load power","readWrite":"RO","type":"U32","unit":"W","gain":"ONE","registerAddress":{"value":37500},"quantity":{"value":2},"scope":{"value":""},"category":"DYNAMIC"},"value":885},{"definition":{"no":37502,"name":"Grid power","readWrite":"RO","type":"I32","unit":"W","gain":"ONE","registerAddress":{"value":37502},"quantity":{"value":2},"scope":{"value":""},"category":"DYNAMIC"},"value":-6089},{"definition":{"no":37504,"name":"Battery power","readWrite":"RO","type":"I32","unit":"W","gain":"ONE","registerAddress":{"value":37504},"quantity":{"value":2},"scope":{"value":""},"category":"DYNAMIC"},"value":0},{"definition":{"no":37516,"name":"Total active power","readWrite":"RO","type":"I32","unit":"KW","gain":"THOUSAND","registerAddress":{"value":37516},"quantity":{"value":2},"scope":{"value":""},"category":"DYNAMIC"},"value":6974}]}
        """.trimIndent()

        val stored = client.post("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_STORE_MODBUS_VALUES") {
            contentType(ContentType.Application.Json)
            setBody(goWireBody)
        }
        assertEquals(OK, stored.status)

        suspend fun readBack(type: String, address: String) =
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH") {
                url { appendPathSegments(type, "100", address, "fast-dongle-job") }
            }

        readBack("I32", "37516").apply {
            assertEquals(OK, status)
            assertEquals("6974", bodyAsText())
        }
        readBack("U32", "37500").apply {
            assertEquals(OK, status)
            assertEquals("885", bodyAsText())
        }
        readBack("I32", "37502").apply {
            assertEquals(OK, status)
            assertEquals("-6089", bodyAsText())
        }
    }
}
