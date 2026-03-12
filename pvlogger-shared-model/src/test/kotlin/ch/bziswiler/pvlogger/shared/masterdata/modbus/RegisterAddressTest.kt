package ch.bziswiler.pvlogger.shared.masterdata.modbus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegisterAddressTest {

    @Test
    fun `compareTo equal addresses`() {
        assertEquals(0, RegisterAddress(100).compareTo(RegisterAddress(100)))
    }

    @Test
    fun `compareTo less than`() {
        assertTrue(RegisterAddress(100) < RegisterAddress(200))
    }

    @Test
    fun `compareTo greater than`() {
        assertTrue(RegisterAddress(200) > RegisterAddress(100))
    }

    @Test
    fun `sorting a list of register addresses`() {
        val addresses = listOf(RegisterAddress(300), RegisterAddress(100), RegisterAddress(200))
        val sorted = addresses.sorted()
        assertEquals(listOf(RegisterAddress(100), RegisterAddress(200), RegisterAddress(300)), sorted)
    }

    @Test
    fun `data class equality`() {
        assertEquals(RegisterAddress(42), RegisterAddress(42))
    }
}
