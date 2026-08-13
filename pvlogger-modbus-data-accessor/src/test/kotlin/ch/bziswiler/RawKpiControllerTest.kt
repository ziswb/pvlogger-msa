package ch.bziswiler

import ch.bziswiler.pvlogger.dataaccessor.database.access.EnergyToday
import ch.bziswiler.pvlogger.dataaccessor.database.access.RawKpiRegisterValues
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_METER_ENERGY_TODAY
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_RAW_KPI_REGISTER_VALUES
import ch.bziswiler.testutils.TestBase
import ch.bziswiler.testutils.TestDataFactory.cleanAllTables
import ch.bziswiler.testutils.TestDataFactory.seedRawKpiRow
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class RawKpiControllerTest : TestBase() {

    private val json = Json { ignoreUnknownKeys = true }

    // The daily-delta SQL buckets by CAST(date_created AS DATE), which
    // PostgreSQL evaluates in the session timezone (= the JVM's zone via
    // pgJDBC) — so the seeds must be built in that same zone, not UTC.
    private val zone: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.now()
    private val yesterday: LocalDate = today.minusDays(1)

    @Test
    fun `store raw KPI returns 200`() = withApp {
        startApplication()
        cleanAllTables()
        val values = RawKpiRegisterValues(
            jobId = "test-job",
            inverterActivePower = 5000.0,
            meterActivePower = 3000.0,
            inverterDailyYield = 10000.0,
            inverterAccumulatedYield = 90000.0,
            meterExportedEnergy = 500.0,
            meterImportedEnergy = 200.0,
        )
        val response = client.post("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_STORE_RAW_KPI_REGISTER_VALUES") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RawKpiRegisterValues.serializer(), values))
        }
        assertEquals(OK, response.status)
    }

    @Test
    fun `energy today with no data returns zeros and null produced`() = withApp {
        startApplication()
        cleanAllTables()
        val energy = readEnergyToday()
        assertEquals(0.0, energy.exportedEnergyToday)
        assertEquals(0.0, energy.importedEnergyToday)
        assertNull(energy.producedEnergyToday)
    }

    @Test
    fun `energy today computes deltas`() = withApp {
        startApplication()
        cleanAllTables()
        seedRawKpiRow(
            dateCreated = yesterday.atTime(12, 0).atZone(zone).toInstant(),
            meterExportedEnergy = 100.0,
            meterImportedEnergy = 50.0,
            inverterAccumulatedYield = 90000.0,
        )
        seedRawKpiRow(
            dateCreated = today.atTime(8, 0).atZone(zone).toInstant(),
            meterExportedEnergy = 120.0,
            meterImportedEnergy = 70.0,
            inverterAccumulatedYield = 90010.0,
        )
        seedRawKpiRow(
            dateCreated = today.atTime(12, 0).atZone(zone).toInstant(),
            meterExportedEnergy = 150.0,
            meterImportedEnergy = 90.0,
            inverterAccumulatedYield = 90025.0,
        )
        val energy = readEnergyToday()
        assertEquals(50.0, energy.exportedEnergyToday)
        assertEquals(40.0, energy.importedEnergyToday)
        assertEquals(25.0, energy.producedEnergyToday)
    }

    @Test
    fun `produced is null while yesterday has no lifetime baseline`() = withApp {
        startApplication()
        cleanAllTables()
        // Pre-V9 rows carry 0.0 in inverter_accumulated_yield; a delta against
        // that would report the counter's full lifetime value as "today".
        seedRawKpiRow(
            dateCreated = yesterday.atTime(12, 0).atZone(zone).toInstant(),
            meterExportedEnergy = 100.0,
            meterImportedEnergy = 50.0,
            inverterAccumulatedYield = 0.0,
        )
        seedRawKpiRow(
            dateCreated = today.atTime(12, 0).atZone(zone).toInstant(),
            meterExportedEnergy = 150.0,
            meterImportedEnergy = 90.0,
            inverterAccumulatedYield = 90025.0,
        )
        val energy = readEnergyToday()
        assertEquals(50.0, energy.exportedEnergyToday)
        assertNull(energy.producedEnergyToday)
    }

    @Test
    fun `delta is anchored on the newest raw row, not on the current date`() = withApp {
        startApplication()
        cleanAllTables()
        // Simulate the midnight race: the newest raw row was stored just before
        // midnight, the delta is queried just after. The result must be that
        // row's day-total, not a bogus window based on the query-time date.
        val dayBefore = yesterday.minusDays(1)
        seedRawKpiRow(
            dateCreated = dayBefore.atTime(23, 59).atZone(zone).toInstant(),
            meterExportedEnergy = 100.0,
            meterImportedEnergy = 50.0,
            inverterAccumulatedYield = 90000.0,
        )
        seedRawKpiRow(
            dateCreated = yesterday.atTime(23, 59).atZone(zone).toInstant(),
            meterExportedEnergy = 130.0,
            meterImportedEnergy = 65.0,
            inverterAccumulatedYield = 90040.0,
        )
        val energy = readEnergyToday()
        assertEquals(30.0, energy.exportedEnergyToday)
        assertEquals(15.0, energy.importedEnergyToday)
        assertEquals(40.0, energy.producedEnergyToday)
    }

    private suspend fun ApplicationTestBuilder.readEnergyToday(): EnergyToday =
        json.decodeFromString<EnergyToday>(
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_METER_ENERGY_TODAY").bodyAsText()
        )
}
