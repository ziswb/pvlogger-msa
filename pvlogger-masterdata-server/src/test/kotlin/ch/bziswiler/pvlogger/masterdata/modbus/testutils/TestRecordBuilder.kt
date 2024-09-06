package ch.bziswiler.pvlogger.masterdata.modbus.testutils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Gain
import ch.bziswiler.pvlogger.shared.masterdata.modbus.ReadWrite
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

object TestRecordBuilder {
    fun createTestRecords(
        number: Int = 1,
        name: String = "TestRecord 1",
        readWrite: ReadWrite = ReadWrite.RO,
        type: Type = Type.I16,
        unit: Unit = Unit.KWH,
        gain: Gain = Gain.ONE,
        address: Int = 1,
        quantity: Int = 1,
        scope: String = "scope",
        category: Category = Category.ALARM,
    ): List<CSVRecord> =
        StringBuilder()
            .append(number).append(",")
            .append(name).append(",")
            .append(readWrite.name).append(",")
            .append(type.name).append(",")
            .append(unit.raw.first()).append(",")
            .append(gain.raw.first()).append(",")
            .append(address).append(",")
            .append(quantity).append(",")
            .append(scope).append(",")
            .append(category.name)
            .toString()
            .let { CSVParser.parse(it, CSVFormat.DEFAULT) }
            .run { this.records }
}
