package ch.bziswiler

import ch.bziswiler.pvlogger.dataaccessor.database.access.MeterEnergyToday
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
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RawKpiControllerTest : TestBase() {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `store raw KPI returns 200`() = withApp {
        startApplication()
        cleanAllTables()
        val values = RawKpiRegisterValues(
            jobId = "test-job",
            inverterActivePower = 5000.0,
            meterActivePower = 3000.0,
            inverterDailyYield = 10000.0,
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
    fun `meter energy today with no data returns zeros`() = withApp {
        startApplication()
        cleanAllTables()
        val energy = json.decodeFromString<MeterEnergyToday>(
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_METER_ENERGY_TODAY").bodyAsText()
        )
        assertEquals(0.0, energy.exportedEnergyToday)
        assertEquals(0.0, energy.importedEnergyToday)
    }

    @Test
    fun `meter energy today computes delta`() = withApp {
        startApplication()
        cleanAllTables()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        seedRawKpiRow(
            dateCreated = yesterday.atTime(23, 0),
            meterExportedEnergy = 100.0,
            meterImportedEnergy = 50.0,
        )
        seedRawKpiRow(
            dateCreated = today.atTime(8, 0),
            meterExportedEnergy = 120.0,
            meterImportedEnergy = 70.0,
        )
        seedRawKpiRow(
            dateCreated = today.atTime(12, 0),
            meterExportedEnergy = 150.0,
            meterImportedEnergy = 90.0,
        )
        val energy = json.decodeFromString<MeterEnergyToday>(
            client.get("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_METER_ENERGY_TODAY").bodyAsText()
        )
        assertEquals(50.0, energy.exportedEnergyToday)
        assertEquals(40.0, energy.importedEnergyToday)
    }
}
