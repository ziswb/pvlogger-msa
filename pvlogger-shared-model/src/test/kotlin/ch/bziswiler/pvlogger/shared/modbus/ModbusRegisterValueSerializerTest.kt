package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Gain
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Quantity
import ch.bziswiler.pvlogger.shared.masterdata.modbus.ReadWrite
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Scope
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ModbusRegisterValueSerializerTest {

    private val json = Json {
        serializersModule = SerializersModule {
            contextual(RegisterAddress.serializer())
            contextual(Quantity.serializer())
            contextual(Scope.serializer())
        }
    }

    private fun definition(type: Type) = ModbusRegisterDefinition(
        no = 1,
        name = "test-register",
        readWrite = ReadWrite.RO,
        type = type,
        unit = Unit.W,
        gain = Gain.ONE,
        registerAddress = RegisterAddress(30000),
        quantity = Quantity(1),
        scope = Scope("device"),
        category = Category.DYNAMIC,
    )

    private fun encode(value: ModbusRegisterValue<*>): String =
        json.encodeToString(ModbusRegisterValueSerializer, value)

    private fun decode(jsonStr: String): ModbusRegisterValue<*> =
        json.decodeFromString(ModbusRegisterValueSerializer, jsonStr)

    @Test
    fun `roundtrip UShort value`() {
        val original = ModbusRegisterUShortValue(definition(Type.U16), 42u)
        val deserialized = decode(encode(original))

        assertIs<ModbusRegisterUShortValue>(deserialized)
        assertEquals(42u.toUShort(), deserialized.value)
    }

    @Test
    fun `roundtrip UInt value`() {
        val original = ModbusRegisterUIntValue(definition(Type.U32), 100000u)
        val deserialized = decode(encode(original))

        assertIs<ModbusRegisterUIntValue>(deserialized)
        assertEquals(100000u, deserialized.value)
    }

    @Test
    fun `roundtrip Short value`() {
        val original = ModbusRegisterShortValue(definition(Type.I16), (-123).toShort())
        val deserialized = decode(encode(original))

        assertIs<ModbusRegisterShortValue>(deserialized)
        assertEquals((-123).toShort(), deserialized.value)
    }

    @Test
    fun `roundtrip Int value`() {
        val original = ModbusRegisterIntValue(definition(Type.I32), -50000)
        val deserialized = decode(encode(original))

        assertIs<ModbusRegisterIntValue>(deserialized)
        assertEquals(-50000, deserialized.value)
    }

    @Test
    fun `roundtrip String value`() {
        val original = ModbusRegisterStringValue(definition(Type.STR), "SUN2000")
        val deserialized = decode(encode(original))

        assertIs<ModbusRegisterStringValue>(deserialized)
        assertEquals("SUN2000", deserialized.value)
    }

    @Test
    fun `roundtrip ShortArray value`() {
        val original = ModbusRegisterShortArrayValue(definition(Type.MLD), shortArrayOf(1, 2, 3))
        val deserialized = decode(encode(original))

        assertIs<ModbusRegisterShortArrayValue>(deserialized)
        assertEquals(3, deserialized.value.size)
        assertEquals(1, deserialized.value[0])
        assertEquals(2, deserialized.value[1])
        assertEquals(3, deserialized.value[2])
    }

    @Test
    fun `BITFIELD_16 deserializes as UShort`() {
        val original = ModbusRegisterUShortValue(definition(Type.BITFIELD_16), 255u)
        val deserialized = decode(encode(original))

        assertIs<ModbusRegisterUShortValue>(deserialized)
    }

    @Test
    fun `BITFIELD_32 deserializes as UInt`() {
        val original = ModbusRegisterUIntValue(definition(Type.BITFIELD_32), 65535u)
        val deserialized = decode(encode(original))

        assertIs<ModbusRegisterUIntValue>(deserialized)
    }

    @Test
    fun `definition is preserved through serialization`() {
        val def = definition(Type.U16)
        val original = ModbusRegisterUShortValue(def, 99u)
        val deserialized = decode(encode(original))

        assertEquals(def.name, deserialized.definition.name)
        assertEquals(def.type, deserialized.definition.type)
        assertEquals(def.registerAddress, deserialized.definition.registerAddress)
    }
}
