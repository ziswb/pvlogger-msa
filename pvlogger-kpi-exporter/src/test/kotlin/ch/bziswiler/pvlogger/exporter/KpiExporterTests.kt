package ch.bziswiler.pvlogger.exporter

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime.Companion.Format
import kotlinx.datetime.TimeZone.Companion.UTC
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.isSymbolicLink
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KpiExporterTests {


    @Test
    fun `learning test for kotlinx datetime`() {
        val instant = Instant.fromEpochMilliseconds(1736204537531)
        val localDateTime = instant.toLocalDateTime(UTC)
        // "yyyy-MM-dd HH:mm:ss"
        val formatter = Format { year();char('-');monthNumber();char('-');dayOfMonth();char(' ');hour();char(':');minute();char(':');second() }
        val formatted = formatter.format(localDateTime)
        assertEquals("2025-01-06 23:02:17", formatted)
    }

    @Test
    fun `learning test for kotlinx datetime 2`() {
        val instant = Instant.fromEpochMilliseconds(1736204537531)
        val localDateTime = instant.toLocalDateTime(UTC)
        // "yyyy-MM-dd_HH-mm-ss-SSS"
        val formatter = Format { year();char('-');monthNumber();char('-');dayOfMonth();char('_');hour();char('-');minute();char('-');second();char('-');secondFraction(3) }
        val formatted = formatter.format(localDateTime)
        assertEquals("2025-01-06_23-02-17-531", formatted)
    }

    @Test
    fun `learning test for symbolic links`() {
        val file = File(File("/Users/bziswiler/pv-data7", "pvlogger_export"), "some_file.json")
        assertTrue { file.parentFile.toPath().isSymbolicLink() }
        assertTrue { file.parentFile.exists() }
    }
}