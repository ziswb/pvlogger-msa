package ch.bziswiler.pvlogger.masterdata.modbus

import ch.bziswiler.pvlogger.masterdata.modbus.testutils.BaseApplicationTest
import ch.bziswiler.pvlogger.masterdata.modbus.testutils.TestRecordBuilder
import ch.bziswiler.pvlogger.masterdata.server.configuration.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.database.access.MasterDataDao
import ch.bziswiler.pvlogger.shared.rest.MasterDataPaths.MASTER_DATA_FOR_ADDRESS_PATH
import ch.bziswiler.pvlogger.shared.rest.MasterDataPaths.MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES
import ch.bziswiler.pvlogger.masterdata.server.service.MasterDataImporter
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.ALARM
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.DEVLST
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.DYNAMIC
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType.DONGLE
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType.INVERTER
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.OK
import kotlinx.serialization.json.Json.Default.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.test.inject
import kotlin.test.assertNotNull

class MasterDataServiceTest : BaseApplicationTest() {

    private val appConfig by inject<AppConfig>()
    private val dao by inject<MasterDataDao>()

    private suspend fun setUpDatabase() {
        val csvRecords = TestRecordBuilder.createTestRecords()
        MasterDataImporter(appConfig, dao).storeMasterData(INVERTER, csvRecords)
    }

    @Test
    fun `test get master data for categories and device types`() = withApp {
        startApplication()
        setUpDatabase()
        client.get("/$MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES") {
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
    fun `test get non-existing master data for categories and device types`() = withApp {
        startApplication()
        setUpDatabase()
        client.get("/$MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES") {
            url {
                parameters.append("deviceTypes", listOf(INVERTER.name).joinToString(separator = ";"))
                parameters.append("categories", listOf(DEVLST.name).joinToString(separator = ";"))
            }
        }.apply {
            assertEquals(NoContent, status)
            assertNotNull(bodyAsText())
        }
    }

    @Test
    fun `test get master data for non-existing categories and device types`() = withApp {
        client.get("/$MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES") {
            url {
                parameters.append("categories", listOf("foo", "bar", "baz").joinToString(separator = ";"))
            }
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `test get master data for address`() = withApp {
        val address = 1
        startApplication()
        setUpDatabase()
        client.get("/$MASTER_DATA_FOR_ADDRESS_PATH/$address").apply {
            assertEquals(OK, status)
            assertNotNull(bodyAsText())
        }
    }

    @Test
    fun `test get non-existing master data for address`() = withApp {
        val address = 10
        startApplication()
        setUpDatabase()
        client.get("/$MASTER_DATA_FOR_ADDRESS_PATH/$address").apply {
            assertEquals(NoContent, status)
            val actual = bodyAsText()
            assertNotNull(actual)
        }
    }

    @Test
    fun `test get master data for the dongle device type`() = withApp {
        startApplication()
        // The dongle rows themselves ship as migration V2 (ADR-0017), which the
        // per-test TRUNCATE wipes — seed a representative row and verify the
        // DONGLE device type round-trips through store, query, and DTO.
        val records = TestRecordBuilder.createTestRecords(
            name = "Load power",
            type = Type.U32,
            unit = Unit.W,
            address = 37500,
            quantity = 2,
            category = DYNAMIC,
        )
        MasterDataImporter(appConfig, dao).storeMasterData(DONGLE, records)
        client.get("/$MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES") {
            url {
                parameters.append("deviceTypes", DONGLE.name)
                parameters.append("categories", DYNAMIC.name)
            }
        }.apply {
            assertEquals(OK, status)
            val result = decodeFromString<Array<ModbusRegisterDefinition>>(bodyAsText())
            assertEquals(37500, result.single().registerAddress.value)
            assertEquals(Type.U32, result.single().type)
        }
    }
}
