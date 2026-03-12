@file:OptIn(ExperimentalUnsignedTypes::class)

package ch.bziswiler.pvlogger.shared.modbus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ModbusObjectTest {

    @Test
    fun `equals with same values`() {
        val a = ModbusObject(1u, 3u, ubyteArrayOf(0x41u, 0x42u, 0x43u))
        val b = ModbusObject(1u, 3u, ubyteArrayOf(0x41u, 0x42u, 0x43u))

        assertEquals(a, b)
    }

    @Test
    fun `equals with different objectValue`() {
        val a = ModbusObject(1u, 3u, ubyteArrayOf(0x41u, 0x42u, 0x43u))
        val b = ModbusObject(1u, 3u, ubyteArrayOf(0x44u, 0x45u, 0x46u))

        assertNotEquals(a, b)
    }

    @Test
    fun `equals with different objectId`() {
        val a = ModbusObject(1u, 3u, ubyteArrayOf(0x41u))
        val b = ModbusObject(2u, 3u, ubyteArrayOf(0x41u))

        assertNotEquals(a, b)
    }

    @Test
    fun `equals with different objectLength`() {
        val a = ModbusObject(1u, 3u, ubyteArrayOf(0x41u))
        val b = ModbusObject(1u, 4u, ubyteArrayOf(0x41u))

        assertNotEquals(a, b)
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = ModbusObject(1u, 3u, ubyteArrayOf(0x41u, 0x42u))
        val b = ModbusObject(1u, 3u, ubyteArrayOf(0x41u, 0x42u))

        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `hashCode differs for different values`() {
        val a = ModbusObject(1u, 3u, ubyteArrayOf(0x41u))
        val b = ModbusObject(1u, 3u, ubyteArrayOf(0x42u))

        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `toString contains decoded string`() {
        val obj = ModbusObject(0u, 3u, ubyteArrayOf(0x41u, 0x42u, 0x43u))
        val str = obj.toString()

        assertTrue(str.contains("ABC"), "Expected 'ABC' in toString, got: $str")
    }

    @Test
    fun `equals same reference`() {
        val a = ModbusObject(1u, 1u, ubyteArrayOf(0x00u))
        assertEquals(a, a)
    }
}
