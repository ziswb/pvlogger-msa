package ch.bziswiler

import ch.bziswiler.pvlogger.dataaccessor.database.access.PvLoggerKpiData
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_KPI_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_KPI_VALUES
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import ch.bziswiler.pvlogger.shared.unit.W
import ch.bziswiler.pvlogger.shared.unit.Wh
import ch.bziswiler.testutils.TestBase
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.TimeZone

/**
 * The repeated hour of the autumn DST switch (2026-10-25, 02:00–03:00 local happens twice
 * in Europe/Zurich). Production runs the JVM in Europe/Zurich, so the test does too.
 */
class DstFallBackKpiTest : TestBase() {

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var originalZone: TimeZone

    @BeforeAll
    fun useProductionZone() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Zurich"))
    }

    @AfterAll
    fun restoreZone() = TimeZone.setDefault(originalZone)

    @Test
    fun `reading from the second pass of the repeated hour is the latest`() = withApp {
        startApplication()
        val firstPass = Instant.parse("2026-10-25T00:59:00Z")  // 02:59 CEST
        val secondPass = Instant.parse("2026-10-25T01:30:00Z") // 02:30 CET, 31 minutes later

        store(firstPass)
        store(secondPass)

        val latest = json.decodeFromString<KpiValuesDto>(
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_KPI_PATH").bodyAsText()
        )
        assertEquals(secondPass.toEpochMilli(), latest.createdAt)
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.store(createdAt: Instant) {
        val kpiData = PvLoggerKpiData(
            jobId = "dst-$createdAt",
            dateCreated = createdAt.toEpochMilli(),
            producing = W(0.0),
            exporting = W(0.0),
            importing = W(500.0),
            consuming = W(500.0),
            selfConsuming = W(0.0),
            produced = Wh(0.0),
            exported = Wh(0.0),
            imported = Wh(1000.0),
            consumed = Wh(1000.0),
            selfConsumed = Wh(0.0),
        )
        val response = client.post("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_STORE_KPI_VALUES") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(PvLoggerKpiData.serializer(), kpiData))
        }
        assertEquals(OK, response.status)
    }
}
