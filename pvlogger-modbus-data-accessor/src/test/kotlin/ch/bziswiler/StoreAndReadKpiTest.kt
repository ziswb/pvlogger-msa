package ch.bziswiler

import ch.bziswiler.pvlogger.dataaccessor.database.access.PvLoggerKpiData
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_KPI_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_KPI_VALUES
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import ch.bziswiler.pvlogger.shared.unit.W
import ch.bziswiler.pvlogger.shared.unit.Wh
import ch.bziswiler.testutils.TestBase
import ch.bziswiler.testutils.TestDataFactory.cleanAllTables
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StoreAndReadKpiTest : TestBase() {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `store KPI values round trip`() = withApp {
        startApplication()
        cleanAllTables()
        val now = System.currentTimeMillis()
        val kpiData = PvLoggerKpiData(
            jobId = "test-job",
            dateCreated = now,
            producing = W(5000.0),
            exporting = W(2000.0),
            importing = W(1000.0),
            consuming = W(4000.0),
            selfConsuming = W(3000.0),
            produced = Wh(10000.0),
            exported = Wh(4000.0),
            imported = Wh(2000.0),
            consumed = Wh(8000.0),
            selfConsumed = Wh(6000.0),
        )
        val storeResponse = client.post("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_STORE_KPI_VALUES") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(PvLoggerKpiData.serializer(), kpiData))
        }
        assertEquals(OK, storeResponse.status)

        val kpi = json.decodeFromString<KpiValuesDto>(
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_KPI_PATH").bodyAsText()
        )
        assertEquals(5.0, kpi.solarPower)
        assertEquals(3.0, kpi.solarToHousePower)
        assertEquals(2.0, kpi.solarToGridPower)
        assertEquals(1.0, kpi.gridToHousePower)
        assertEquals(4.0, kpi.ownConsumption)
        assertEquals(10.0, kpi.dailyEnergyYield)
        assertEquals(4.0, kpi.dailyEnergyExported)
        assertEquals(2.0, kpi.dailyEnergyImported)
        assertEquals(8.0, kpi.dailyEnergyConsumed)
        assertEquals(6.0, kpi.dailyEnergySelfCovered)
    }

    @Test
    fun `malformed JSON returns 400`() = withApp {
        startApplication()
        cleanAllTables()
        val response = client.post("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_STORE_KPI_VALUES") {
            contentType(ContentType.Application.Json)
            setBody("{not valid json}")
        }
        assertEquals(BadRequest, response.status)
        assertEquals("Bad request", response.bodyAsText())
    }
}
