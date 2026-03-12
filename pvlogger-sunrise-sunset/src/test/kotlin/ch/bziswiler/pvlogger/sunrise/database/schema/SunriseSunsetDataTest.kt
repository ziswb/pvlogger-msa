package ch.bziswiler.pvlogger.sunrise.database.schema

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SunriseSunsetDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sampleApiResponse = """
        {
          "results": {
            "sunrise": "2025-02-12T06:32:32+00:00",
            "sunset": "2025-02-12T16:44:32+00:00",
            "solar_noon": "2025-02-12T11:38:32+00:00",
            "day_length": 36720,
            "civil_twilight_begin": "2025-02-12T06:02:18+00:00",
            "civil_twilight_end": "2025-02-12T17:14:46+00:00",
            "nautical_twilight_begin": "2025-02-12T05:26:09+00:00",
            "nautical_twilight_end": "2025-02-12T17:50:55+00:00",
            "astronomical_twilight_begin": "2025-02-12T04:50:30+00:00",
            "astronomical_twilight_end": "2025-02-12T18:26:34+00:00"
          },
          "status": "OK",
          "tzid": "UTC"
        }
    """.trimIndent()

    @Test
    fun `deserialize full API response`() {
        val data = json.decodeFromString<SunriseSunsetData>(sampleApiResponse)

        assertEquals("OK", data.status)
        assertEquals("UTC", data.tzid)
    }

    @Test
    fun `deserialize results sunrise and sunset`() {
        val data = json.decodeFromString<SunriseSunsetData>(sampleApiResponse)

        assertEquals("2025-02-12T06:32:32+00:00", data.results.sunrise)
        assertEquals("2025-02-12T16:44:32+00:00", data.results.sunset)
    }

    @Test
    fun `deserialize results day length`() {
        val data = json.decodeFromString<SunriseSunsetData>(sampleApiResponse)

        assertEquals(36720, data.results.day_length)
    }

    @Test
    fun `deserialize results solar noon`() {
        val data = json.decodeFromString<SunriseSunsetData>(sampleApiResponse)

        assertEquals("2025-02-12T11:38:32+00:00", data.results.solar_noon)
    }

    @Test
    fun `deserialize results twilight times`() {
        val data = json.decodeFromString<SunriseSunsetData>(sampleApiResponse)

        assertEquals("2025-02-12T06:02:18+00:00", data.results.civil_twilight_begin)
        assertEquals("2025-02-12T17:14:46+00:00", data.results.civil_twilight_end)
        assertEquals("2025-02-12T05:26:09+00:00", data.results.nautical_twilight_begin)
        assertEquals("2025-02-12T17:50:55+00:00", data.results.nautical_twilight_end)
        assertEquals("2025-02-12T04:50:30+00:00", data.results.astronomical_twilight_begin)
        assertEquals("2025-02-12T18:26:34+00:00", data.results.astronomical_twilight_end)
    }

    @Test
    fun `serialize and deserialize roundtrip`() {
        val original = SunriseSunsetData(
            results = SunriseSunsetDataResult(
                sunrise = "2025-06-21T04:30:00+00:00",
                sunset = "2025-06-21T19:30:00+00:00",
                solar_noon = "2025-06-21T12:00:00+00:00",
                day_length = 54000,
                civil_twilight_begin = "2025-06-21T04:00:00+00:00",
                civil_twilight_end = "2025-06-21T20:00:00+00:00",
                nautical_twilight_begin = "2025-06-21T03:00:00+00:00",
                nautical_twilight_end = "2025-06-21T21:00:00+00:00",
                astronomical_twilight_begin = "2025-06-21T02:00:00+00:00",
                astronomical_twilight_end = "2025-06-21T22:00:00+00:00"
            ),
            status = "OK",
            tzid = "UTC"
        )

        val serialized = json.encodeToString(SunriseSunsetData.serializer(), original)
        val deserialized = json.decodeFromString<SunriseSunsetData>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `SunriseSunsetDataResult data class equality`() {
        val result1 = SunriseSunsetDataResult(
            sunrise = "2025-02-12T06:32:32+00:00",
            sunset = "2025-02-12T16:44:32+00:00",
            solar_noon = "2025-02-12T11:38:32+00:00",
            day_length = 36720,
            civil_twilight_begin = "2025-02-12T06:02:18+00:00",
            civil_twilight_end = "2025-02-12T17:14:46+00:00",
            nautical_twilight_begin = "2025-02-12T05:26:09+00:00",
            nautical_twilight_end = "2025-02-12T17:50:55+00:00",
            astronomical_twilight_begin = "2025-02-12T04:50:30+00:00",
            astronomical_twilight_end = "2025-02-12T18:26:34+00:00"
        )

        val result2 = result1.copy()

        assertEquals(result1, result2)
    }
}
