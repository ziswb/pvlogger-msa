package ch.bziswiler.pvlogger.masterdata.modbus

import ch.bziswiler.pvlogger.masterdata.modbus.testutils.BaseApplicationTest
import ch.bziswiler.pvlogger.masterdata.modbus.testutils.TestRecordBuilder.createTestRecords
import ch.bziswiler.pvlogger.masterdata.server.configuration.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.database.access.MasterDataDao
import ch.bziswiler.pvlogger.masterdata.server.database.schema.MasterDataTable
import ch.bziswiler.pvlogger.masterdata.server.service.MasterDataImporter
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.koin.test.inject
import kotlin.test.assertEquals

class MasterDataImporterTest : BaseApplicationTest() {
    private val appConfig by inject<AppConfig>()
    private val dao by inject<MasterDataDao>()

    @Test
    fun `test masterdata importer`() =
        withApp {
            startApplication()
            val csvRecords = createTestRecords()
            MasterDataImporter(appConfig, dao).storeMasterData(DeviceType.INVERTER, csvRecords)
            var name = ""
            transaction { name = MasterDataTable.selectAll().first()[MasterDataTable.name] }
            assertEquals("TestRecord 1", name, "field 'name' contained the value '$name' but should contain 'TestRecord 1'")
        }
}
