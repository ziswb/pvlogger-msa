package ch.bziswiler

import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_KPI_PATH
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import ch.bziswiler.testutils.TestBase
import ch.bziswiler.testutils.TestDataFactory.cleanAllTables
import ch.bziswiler.testutils.TestDataFactory.seedKpiRow
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class HouseKpiControllerTest : TestBase() {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `today KPIs empty DB returns empty list`() = withApp {
        startApplication()
        cleanAllTables()
        val response = client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH")
        assertEquals(OK, response.status)
        val kpis = json.decodeFromString<List<KpiValuesDto>>(response.bodyAsText())
        assertTrue(kpis.isEmpty())
    }

    @Test
    fun `today KPIs filters by date`() = withApp {
        startApplication()
        cleanAllTables()
        val now = LocalDateTime.now(ZoneOffset.UTC)
        seedKpiRow(dateCreated = now.minusDays(1), solarPower = 1.0)
        seedKpiRow(dateCreated = now, solarPower = 2.0)
        val kpis = json.decodeFromString<List<KpiValuesDto>>(
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH").bodyAsText()
        )
        assertEquals(1, kpis.size)
        assertEquals(2.0, kpis[0].solarPower)
    }

    @Test
    fun `KPIs for specific date`() = withApp {
        startApplication()
        cleanAllTables()
        seedKpiRow(dateCreated = LocalDateTime.of(2025, 6, 15, 10, 0), solarPower = 3.5)
        seedKpiRow(dateCreated = LocalDateTime.of(2025, 6, 15, 12, 0), solarPower = 4.5)
        seedKpiRow(dateCreated = LocalDateTime.of(2025, 6, 16, 10, 0), solarPower = 9.0)
        val kpis = json.decodeFromString<List<KpiValuesDto>>(
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH/2025-6-15").bodyAsText()
        )
        assertEquals(2, kpis.size)
        assertEquals(3.5, kpis[0].solarPower)
        assertEquals(4.5, kpis[1].solarPower)
    }

    @Test
    fun `invalid date returns 400`() = withApp {
        startApplication()
        cleanAllTables()
        val response = client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH/not-a-date")
        assertEquals(BadRequest, response.status)
        assertEquals("Invalid date format", response.bodyAsText())
    }

    @Test
    fun `latest KPI returns newest`() = withApp {
        startApplication()
        cleanAllTables()
        val now = LocalDateTime.now(ZoneOffset.UTC)
        seedKpiRow(dateCreated = now.minusHours(1), solarPower = 1.0)
        seedKpiRow(dateCreated = now, solarPower = 5.0)
        val kpi = json.decodeFromString<KpiValuesDto>(
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_KPI_PATH").bodyAsText()
        )
        assertEquals(5.0, kpi.solarPower)
    }

    @Test
    fun `latest KPI returns 404 when empty`() = withApp {
        startApplication()
        cleanAllTables()
        val response = client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_KPI_PATH")
        assertEquals(NotFound, response.status)
    }

    @Test
    fun `gridPower is gridToHouse minus solarToGrid`() = withApp {
        startApplication()
        cleanAllTables()
        seedKpiRow(
            dateCreated = LocalDateTime.now(ZoneOffset.UTC),
            gridToHousePower = 3.456,
            solarToGridPower = 1.234,
        )
        val kpi = json.decodeFromString<KpiValuesDto>(
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_KPI_PATH").bodyAsText()
        )
        assertEquals(2.22, kpi.gridPower)
        assertEquals(3.46, kpi.gridToHousePower)
        assertEquals(1.23, kpi.solarToGridPower)
    }
}
