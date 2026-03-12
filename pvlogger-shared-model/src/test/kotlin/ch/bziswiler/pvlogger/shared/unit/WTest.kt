package ch.bziswiler.pvlogger.shared.unit

import kotlin.test.Test
import kotlin.test.assertEquals

class WTest {

    @Test
    fun `toKW divides by 1000`() {
        assertEquals(KW(1.5), W(1500.0).toKW())
    }

    @Test
    fun `toKW zero`() {
        assertEquals(KW(0.0), W(0.0).toKW())
    }

    @Test
    fun `plus W`() {
        assertEquals(W(300.0), W(100.0) + W(200.0))
    }

    @Test
    fun `plus KW converts to W then adds`() {
        assertEquals(W(1500.0), W(500.0) + KW(1.0))
    }

    @Test
    fun `minus W`() {
        assertEquals(W(100.0), W(300.0) - W(200.0))
    }

    @Test
    fun `minus KW converts to W then subtracts`() {
        assertEquals(W(500.0), W(1500.0) - KW(1.0))
    }

    @Test
    fun `toKW and back roundtrip`() {
        val original = W(1234.0)
        assertEquals(original, original.toKW().toW())
    }
}
