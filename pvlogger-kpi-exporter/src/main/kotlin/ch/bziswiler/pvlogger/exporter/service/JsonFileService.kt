package ch.bziswiler.pvlogger.exporter.service

import ch.bziswiler.pvlogger.exporter.configuration.ExportConfig
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.time.Instant.ofEpochMilli
import java.time.LocalDateTime.from
import java.time.LocalDateTime.parse
import java.time.ZoneId.systemDefault
import java.time.format.DateTimeFormatter.ofPattern

class JsonFileService(exportConfig: ExportConfig, private val directoryService: DirectoryService) : ExportService {

    private val logger = KotlinLogging.logger {}
    private val fileNameDatePattern = ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")
    private val lastUpdateTimePattern = ofPattern("yyyy-MM-dd HH:mm:ss")
    private val path = exportConfig.path
    private val name = exportConfig.name
    private val myJson = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        encodeDefaults = true
    }

    override fun export(data: KpiValuesDto) {
        write(
            KpiExportRecord(
                lastUpdateTime = from(ofEpochMilli(data.createdAt).atZone(systemDefault())).format(lastUpdateTimePattern),
                CurrentDataRecord(
                    pv_prod_akt_w = data.solarPower,
                    pv_verk_akt_w = data.solarToGridPower,
                    pv_kauf_akt_w = data.gridToHousePower,
                    pv_verb_akt_w = data.solarToHousePower + data.gridToHousePower,
                ), CurrentDayDataRecord(
                    pv_prod_tag_wh = data.dailyEnergyYield,
                    pv_verk_tag_wh = data.dailyEnergyExported,
                    pv_kauf_tag_wh = data.dailyEnergyImported,
                    pv_verb_tag_wh = data.dailyEnergyConsumed,
                )
            )
        )
        directoryService.cleanUpExportDirectory()
    }

    private fun write(record: KpiExportRecord) {
        logger.info { "appending a record to the JSON file" }
        val file = File(path, "$name-${fileNameCompliantDateTime(record)}.json")
        assertFileExists(file)
        exportRecordToJson(record, file)
    }

    private fun fileNameCompliantDateTime(record: KpiExportRecord): String =
        parse(record.lastUpdateTime, lastUpdateTimePattern).format(fileNameDatePattern)

    private fun assertFileExists(file: File) {
        if (!file.exists()) {
            logger.info { "creating new file '$file'" }
            file.createNewFile()
        }
    }

    private fun exportRecordToJson(record: KpiExportRecord, file: File) =
        myJson.encodeToString(record).let { file.writeText(it) }
}
