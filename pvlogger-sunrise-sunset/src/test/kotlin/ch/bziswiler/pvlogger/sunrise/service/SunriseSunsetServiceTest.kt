package ch.bziswiler.pvlogger.sunrise.service

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SunriseSunsetServiceTest {

    @Test
    fun `toUtc parses ISO 8601 with UTC offset`() {
        val result = SunriseSunsetService.toUtc("2025-02-12T06:32:32+00:00")

        assertEquals(2025, result.year)
        assertEquals(2, result.monthValue)
        assertEquals(12, result.dayOfMonth)
        assertEquals(6, result.hour)
        assertEquals(32, result.minute)
        assertEquals(32, result.second)
        assertEquals(ZoneOffset.UTC, result.offset)
    }

    @Test
    fun `toUtc converts non-UTC offset to UTC`() {
        // +02:00 means the UTC time is 2 hours earlier
        val result = SunriseSunsetService.toUtc("2025-06-15T14:00:00+02:00")

        assertEquals(12, result.hour)
        assertEquals(ZoneOffset.UTC, result.offset)
    }

    @Test
    fun `toSystemTimezone from string converts to system timezone`() {
        val result = SunriseSunsetService.toSystemTimezone("2025-02-12T06:32:32+00:00")

        assertEquals(ZoneId.systemDefault(), result.zone)
        // The instant should be the same, just in a different zone
        val utc = ZonedDateTime.parse("2025-02-12T06:32:32+00:00").withZoneSameInstant(ZoneOffset.UTC)
        assertEquals(utc.toInstant(), result.toInstant())
    }

    @Test
    fun `toSystemTimezone from ZonedDateTime converts zone`() {
        val utcTime = ZonedDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC)
        val result = SunriseSunsetService.toSystemTimezone(utcTime)

        assertEquals(ZoneId.systemDefault(), result.zone)
        assertEquals(utcTime.toInstant(), result.toInstant())
    }

    @Test
    fun `toSystemTimezone preserves instant`() {
        val input = "2025-02-12T06:32:32+00:00"
        val result = SunriseSunsetService.toSystemTimezone(input)
        val original = ZonedDateTime.parse(input)

        assertEquals(original.toInstant(), result.toInstant())
    }

    @Test
    fun `toUtc handles midnight`() {
        val result = SunriseSunsetService.toUtc("2025-01-01T00:00:00+00:00")

        assertEquals(0, result.hour)
        assertEquals(0, result.minute)
        assertEquals(0, result.second)
        assertEquals(1, result.dayOfMonth)
    }

    @Test
    fun `toUtc handles date boundary crossing`() {
        // 01:00 at +03:00 is 22:00 the previous day in UTC
        val result = SunriseSunsetService.toUtc("2025-03-15T01:00:00+03:00")

        assertEquals(22, result.hour)
        assertEquals(14, result.dayOfMonth)
    }

    @Test
    fun `today returns ISO date format`() {
        val result = SunriseSunsetService.today()

        assertNotNull(result)
        // Should match YYYY-MM-DD format
        assertTrue(result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")), "Expected ISO date format, got: $result")
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_DATE), result)
    }
}
