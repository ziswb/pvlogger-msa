package ch.bziswiler.pvlogger.shared.unit

import kotlin.test.Test
import kotlin.test.assertEquals

class KWTest {

    @Test
    fun `toW multiplies by 1000`() {
        assertEquals(W(1500.0), KW(1.5).toW())
    }

    @Test
    fun `toW zero`() {
        assertEquals(W(0.0), KW(0.0).toW())
    }

    @Test
    fun `plus KW`() {
        assertEquals(KW(3.0), KW(1.0) + KW(2.0))
    }

    @Test
    fun `plus W converts to KW then adds`() {
        assertEquals(KW(1.5), KW(1.0) + W(500.0))
    }

    @Test
    fun `minus KW`() {
        assertEquals(KW(1.0), KW(3.0) - KW(2.0))
    }

    @Test
    fun `minus W converts to KW then subtracts`() {
        assertEquals(KW(1.0), KW(1.5) - W(500.0))
    }

    @Test
    fun `toW and back roundtrip`() {
        val original = KW(3.456)
        assertEquals(original, original.toW().toKW())
    }
}
