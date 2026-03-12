package ch.bziswiler.pvlogger.shared.modbus.tcp.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateTimeFromRfc3339SerializerTest {

    @Serializable
    private data class Wrapper(
        @Serializable(with = LocalDateTimeFromRfc3339Serializer::class) val time: LocalDateTime
    )

    private val json = Json

    @Test
    fun `deserialize RFC 3339 UTC string`() {
        val input = """{"time":"2025-02-12T06:32:32Z"}"""
        val result = json.decodeFromString<Wrapper>(input)

        assertEquals(2025, result.time.year)
        assertEquals(2, result.time.monthValue)
        assertEquals(12, result.time.dayOfMonth)
        assertEquals(6, result.time.hour)
        assertEquals(32, result.time.minute)
        assertEquals(32, result.time.second)
    }

    @Test
    fun `deserialize RFC 3339 with offset`() {
        val input = """{"time":"2025-02-12T07:32:32+01:00"}"""
        val result = json.decodeFromString<Wrapper>(input)

        // OffsetDateTime.toLocalDateTime strips the offset
        assertEquals(7, result.time.hour)
        assertEquals(32, result.time.minute)
    }

    @Test
    fun `serialize produces UTC offset string`() {
        val wrapper = Wrapper(time = LocalDateTime.of(2025, 6, 15, 14, 30, 0))
        val result = json.encodeToString(Wrapper.serializer(), wrapper)

        assert(result.contains("2025-06-15T14:30")) { "Expected date in output, got: $result" }
        assert(result.contains("Z") || result.contains("+00:00")) { "Expected UTC offset, got: $result" }
    }

    @Test
    fun `roundtrip serialize then deserialize`() {
        val original = Wrapper(time = LocalDateTime.of(2025, 3, 15, 10, 45, 30))
        val serialized = json.encodeToString(Wrapper.serializer(), original)
        val deserialized = json.decodeFromString<Wrapper>(serialized)

        assertEquals(original.time, deserialized.time)
    }
}
