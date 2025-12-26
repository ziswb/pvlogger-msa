package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

object ModbusRegisterValueSerializer : KSerializer<ModbusRegisterValue<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ModbusRegisterValue")

    override fun deserialize(decoder: Decoder): ModbusRegisterValue<*> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("ModbusRegisterValueSerializer only supports JSON")

        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val definitionObj = jsonObject["definition"]?.jsonObject
            ?: throw SerializationException("Missing 'definition' field")
        val typeStr = (definitionObj["type"] as? JsonPrimitive)?.content
            ?: throw SerializationException("Missing 'definition.type' field")

        val type = Type.Companion.from(typeStr)

        return when (type) {
            Type.U16, Type.BITFIELD_16 -> jsonDecoder.json.decodeFromJsonElement(ModbusRegisterUShortValue.serializer(), jsonObject)
            Type.U32, Type.BITFIELD_32 -> jsonDecoder.json.decodeFromJsonElement(ModbusRegisterUIntValue.serializer(), jsonObject)
            Type.I16 -> jsonDecoder.json.decodeFromJsonElement(ModbusRegisterShortValue.serializer(), jsonObject)
            Type.I32 -> jsonDecoder.json.decodeFromJsonElement(ModbusRegisterIntValue.serializer(), jsonObject)
            Type.STR -> jsonDecoder.json.decodeFromJsonElement(ModbusRegisterStringValue.serializer(), jsonObject)
            Type.MLD -> jsonDecoder.json.decodeFromJsonElement(ModbusRegisterShortArrayValue.serializer(), jsonObject)
        }
    }

    override fun serialize(encoder: Encoder, value: ModbusRegisterValue<*>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("ModbusRegisterValueSerializer only supports JSON")

        val jsonElement = when (value) {
            is ModbusRegisterUShortValue -> jsonEncoder.json.encodeToJsonElement(ModbusRegisterUShortValue.serializer(), value)
            is ModbusRegisterUIntValue -> jsonEncoder.json.encodeToJsonElement(ModbusRegisterUIntValue.serializer(), value)
            is ModbusRegisterShortValue -> jsonEncoder.json.encodeToJsonElement(ModbusRegisterShortValue.serializer(), value)
            is ModbusRegisterIntValue -> jsonEncoder.json.encodeToJsonElement(ModbusRegisterIntValue.serializer(), value)
            is ModbusRegisterStringValue -> jsonEncoder.json.encodeToJsonElement(ModbusRegisterStringValue.serializer(), value)
            is ModbusRegisterShortArrayValue -> jsonEncoder.json.encodeToJsonElement(ModbusRegisterShortArrayValue.serializer(), value)
        }
        jsonEncoder.encodeJsonElement(jsonElement)
    }
}