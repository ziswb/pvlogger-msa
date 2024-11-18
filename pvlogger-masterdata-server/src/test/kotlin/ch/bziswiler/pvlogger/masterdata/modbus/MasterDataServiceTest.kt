package ch.bziswiler.pvlogger.masterdata.modbus

import ch.bziswiler.pvlogger.masterdata.modbus.testutils.BaseApplicationTest
import ch.bziswiler.pvlogger.masterdata.modbus.testutils.TestRecordBuilder
import ch.bziswiler.pvlogger.masterdata.server.config.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.modbus.schema.MasterDataDao
import ch.bziswiler.pvlogger.masterdata.server.modbus.services.MasterDataImporter
import ch.bziswiler.pvlogger.masterdata.server.plugins.MASTER_DATA_FOR_ADDRESS_PATH
import ch.bziswiler.pvlogger.masterdata.server.plugins.MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.ALARM
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType.INVERTER
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import kotlinx.serialization.json.Json.Default.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.test.inject
import kotlin.test.assertNotNull

class MasterDataServiceTest : BaseApplicationTest() {
    private val appConfig by inject<AppConfig>()
    private val dao by inject<MasterDataDao>()

    private fun setUpDatabase() {
        val csvRecords = TestRecordBuilder.createTestRecords()
        MasterDataImporter(appConfig, dao).storeMasterData(INVERTER, csvRecords)
    }

    @Test
    fun testMasterDataService() = withApp {
        startApplication()
        setUpDatabase()
        client.get(MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES) {
            url {
                parameters.append("deviceTypes", listOf(INVERTER.name).joinToString(separator = ";"))
                parameters.append("categories", listOf(ALARM.name).joinToString(separator = ";"))
            }
        }.apply {
            assertEquals(OK, status)
            val string = bodyAsText()
            val result = decodeFromString<Array<ModbusRegisterDefinition>>(string)
            assertEquals(ALARM, result.first().category)
        }
    }

    @Test
    fun `test master data service with illegal query parameters`() = withApp {
        client.get(MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES) {
            url {
                parameters.append("categories", listOf("foo", "bar", "baz").joinToString(separator = ";"))
            }
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun testGetMasterDataForAddress() = withApp {
        val address = 1
        startApplication()
        setUpDatabase()
        client.get(MASTER_DATA_FOR_ADDRESS_PATH.replace("{address}", "$address")).apply {
            assertEquals(OK, status)
            assertNotNull(bodyAsText())
        }
    }
}
