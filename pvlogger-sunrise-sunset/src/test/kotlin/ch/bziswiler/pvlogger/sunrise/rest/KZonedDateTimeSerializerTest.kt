package ch.bziswiler.pvlogger.sunrise.rest

import kotlinx.serialization.json.Json
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KZonedDateTimeSerializerTest {

    private val json = Json

    @Test
    fun `serialize SunriseSunsetPair produces valid JSON`() {
        val pair = SunriseSunsetPair(
            sunrise = ZonedDateTime.of(2025, 2, 12, 7, 32, 32, 0, ZoneId.of("Europe/Zurich")),
            sunset = ZonedDateTime.of(2025, 2, 12, 17, 44, 32, 0, ZoneId.of("Europe/Zurich"))
        )

        val result = json.encodeToString(SunriseSunsetPair.serializer(), pair)

        assertNotNull(result)
        assert(result.contains("sunrise"))
        assert(result.contains("sunset"))
        assert(result.contains("2025-02-12"))
    }

    @Test
    fun `deserialize SunriseSunsetPair from JSON`() {
        val jsonStr = """{"sunrise":"2025-02-12T07:32:32+01:00[Europe/Zurich]","sunset":"2025-02-12T17:44:32+01:00[Europe/Zurich]"}"""

        val pair = json.decodeFromString(SunriseSunsetPair.serializer(), jsonStr)

        assertEquals(7, pair.sunrise.hour)
        assertEquals(32, pair.sunrise.minute)
        assertEquals(17, pair.sunset.hour)
        assertEquals(44, pair.sunset.minute)
        assertEquals(ZoneId.of("Europe/Zurich"), pair.sunrise.zone)
    }

    @Test
    fun `roundtrip serialize then deserialize preserves values`() {
        val original = SunriseSunsetPair(
            sunrise = ZonedDateTime.of(2025, 6, 21, 5, 30, 0, 0, ZoneOffset.UTC),
            sunset = ZonedDateTime.of(2025, 6, 21, 21, 15, 0, 0, ZoneOffset.UTC)
        )

        val serialized = json.encodeToString(SunriseSunsetPair.serializer(), original)
        val deserialized = json.decodeFromString(SunriseSunsetPair.serializer(), serialized)

        assertEquals(original.sunrise.toInstant(), deserialized.sunrise.toInstant())
        assertEquals(original.sunset.toInstant(), deserialized.sunset.toInstant())
    }

    @Test
    fun `serialize UTC timezone`() {
        val pair = SunriseSunsetPair(
            sunrise = ZonedDateTime.of(2025, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC),
            sunset = ZonedDateTime.of(2025, 1, 1, 16, 30, 0, 0, ZoneOffset.UTC)
        )

        val result = json.encodeToString(SunriseSunsetPair.serializer(), pair)

        assert(result.contains("08:00"))
        assert(result.contains("16:30"))
    }

    @Test
    fun `deserialize with different timezone offsets`() {
        val jsonStr = """{"sunrise":"2025-02-12T06:32:32+00:00","sunset":"2025-02-12T17:44:32+01:00"}"""

        val pair = json.decodeFromString(SunriseSunsetPair.serializer(), jsonStr)

        assertEquals(ZoneOffset.UTC, pair.sunrise.offset)
        assertEquals(ZoneOffset.ofHours(1), pair.sunset.offset)
    }
}
