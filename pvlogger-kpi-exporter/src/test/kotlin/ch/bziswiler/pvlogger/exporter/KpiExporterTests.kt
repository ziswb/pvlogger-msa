package ch.bziswiler.pvlogger.exporter

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime.Companion.Format
import kotlinx.datetime.TimeZone.Companion.UTC
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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
}