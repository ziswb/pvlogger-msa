package ch.bziswiler.pvlogger.shared.modbus.tcp.response

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

object LocalDateTimeFromRfc3339Serializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val text = decoder.decodeString()
        val odt = OffsetDateTime.parse(text)
        return odt.toLocalDateTime()
    }

    override fun serialize(
        encoder: Encoder,
        value: LocalDateTime,
    ) {
        val odt = value.atOffset(ZoneOffset.UTC)
        encoder.encodeString(odt.toString())
    }
}
