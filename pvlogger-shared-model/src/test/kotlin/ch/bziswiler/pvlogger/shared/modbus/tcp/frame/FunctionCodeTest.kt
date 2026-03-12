package ch.bziswiler.pvlogger.shared.modbus.tcp.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FunctionCodeTest {

    @Test
    fun `from ReadRegisters`() {
        val result = FunctionCode.from(0x03u)
        assertIs<FunctionCode.ReadRegisters>(result)
        assertEquals(0x03u.toUByte(), result.value)
    }

    @Test
    fun `from WriteSingleRegister`() {
        val result = FunctionCode.from(0x06u)
        assertIs<FunctionCode.WriteSingleRegister>(result)
    }

    @Test
    fun `from WriteMultipleRegisters`() {
        val result = FunctionCode.from(0x10u)
        assertIs<FunctionCode.WriteMultipleRegisters>(result)
    }

    @Test
    fun `from ReadDeviceIdentifiers`() {
        val result = FunctionCode.from(0x2Bu)
        assertIs<FunctionCode.ReadDeviceIdentifiers>(result)
    }

    @Test
    fun `from AbnormalResponse`() {
        val result = FunctionCode.from(0x83u)
        assertIs<FunctionCode.AbnormalResponse>(result)
    }

    @Test
    fun `from unknown code returns UnknownResponse`() {
        val result = FunctionCode.from(0xFFu)
        assertIs<FunctionCode.UnknownResponse>(result)
    }

    @Test
    fun `from another unknown code returns UnknownResponse`() {
        val result = FunctionCode.from(0x01u)
        assertIs<FunctionCode.UnknownResponse>(result)
    }

    @Test
    fun `UnknownResponse has value 0`() {
        assertEquals(0x00u.toUByte(), FunctionCode.UnknownResponse.value)
    }
}
