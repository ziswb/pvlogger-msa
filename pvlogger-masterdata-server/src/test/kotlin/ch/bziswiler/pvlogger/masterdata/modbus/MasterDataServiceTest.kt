package ch.bziswiler.pvlogger.masterdata.modbus

import ch.bziswiler.pvlogger.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.masterdata.modbus.testutils.BaseApplicationTest
import ch.bziswiler.pvlogger.masterdata.modbus.testutils.TestRecordBuilder
import ch.bziswiler.pvlogger.masterdata.server.config.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.modbus.schema.MasterDataDao
import ch.bziswiler.pvlogger.masterdata.server.modbus.services.MasterDataImporter
import ch.bziswiler.pvlogger.masterdata.server.plugins.REGISTER_DEFINITIONS_PATH
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.test.inject

class MasterDataServiceTest : BaseApplicationTest() {
    private val appConfig by inject<AppConfig>()
    private val dao by inject<MasterDataDao>()

    private fun setUpDatabase() {
        val csvRecords = TestRecordBuilder.createTestRecords()
        MasterDataImporter(appConfig, dao).storeMasterData(DeviceType.INVERTER, csvRecords)
    }

    @Test
    fun testMasterDataService() =
        withApp {
            startApplication()
            setUpDatabase()
            client.get(REGISTER_DEFINITIONS_PATH) {
                url {
                    parameters.append("deviceTypes", listOf("INVERTER").joinToString(separator = ";"))
                    parameters.append("categories", listOf("ALARM").joinToString(separator = ";"))
                }
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val string = bodyAsText()
                val result = Json.decodeFromString<Array<ModbusRegisterDefinition>>(string)
                assertEquals(Category.ALARM, result.first().category)
            }
        }

    @Test
    fun `test master data service with illegal query parameters`() =
        withApp {
            client.get(REGISTER_DEFINITIONS_PATH) {
                url {
                    parameters.append("categories", listOf("foo", "bar", "baz").joinToString(separator = ";"))
                }
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
            }
        }
}
