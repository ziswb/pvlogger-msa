package ch.bziswiler.pvlogger.masterdata.server.modbus.services

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType.INVERTER
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType.METER
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Gain
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Quantity
import ch.bziswiler.pvlogger.shared.masterdata.modbus.ReadWrite
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Scope
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.masterdata.server.config.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.database.access.MasterDataDao
import java.io.FileInputStream
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord

private const val IDX_NO = 0
private const val IDX_NAME = 1
private const val IDX_RW = 2
private const val IDX_TYPE = 3
private const val IDX_UNIT = 4
private const val IDX_GAIN = 5
private const val IDX_RA = 6
private const val IDX_QUANTITY = 7
private const val IDX_SCOPE = 8
private const val IDX_CATEGORY = 9

class MasterDataImporter(appConfig: AppConfig, private val dao: MasterDataDao) {
    private val meterMasterDataPath = appConfig.masterDataImportConfig.meterMasterDataPath
    private val inverterMasterDataPath = appConfig.masterDataImportConfig.inverterMasterDataPath
    private val devIdsMasterDataPath = appConfig.masterDataImportConfig.deviceIdentifiersMasterDataPath

    fun importMeterRegister() = storeMasterData(METER, readMasterData(meterMasterDataPath))

    fun importInverterRegister() = storeMasterData(INVERTER, readMasterData(inverterMasterDataPath))

    fun importDeviceIdentifiers() = storeMasterData(INVERTER, readMasterData(devIdsMasterDataPath))

    private fun readMasterData(path: String): List<CSVRecord> {
        val csvRecords =
            CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .apply { setIgnoreSurroundingSpaces(true) }
                .build().parse(FileInputStream(path).reader())
                .drop(IDX_NAME) // Dropping the header
        return csvRecords
    }

    fun storeMasterData(
        devType: DeviceType,
        csvRecord: List<CSVRecord>,
    ) = dao.insertBatch(devType, mapToModbusRegisterDefinitions(csvRecord))

    private fun mapToModbusRegisterDefinitions(csvRecords: List<CSVRecord>) =
        csvRecords
            .map {
                ModbusRegisterDefinition(
                    no = it[IDX_NO].toInt(),
                    name = it[IDX_NAME],
                    readWrite = ReadWrite.valueOf(it[IDX_RW]),
                    type = Type.from(it[IDX_TYPE]),
                    unit = Unit.from(it[IDX_UNIT]),
                    gain = Gain.from(it[IDX_GAIN]),
                    registerAddress = RegisterAddress(it[IDX_RA].toInt()),
                    quantity = Quantity(it[IDX_QUANTITY].toInt()),
                    scope = Scope(it[IDX_SCOPE].orEmpty()),
                    category = Category.valueOf(it[IDX_CATEGORY]),
                )
            }.toList().sortedBy(ModbusRegisterDefinition::no)
}
