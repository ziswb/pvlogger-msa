package ch.bziswiler.pvlogger.shared.unit

import kotlin.test.Test
import kotlin.test.assertEquals

class WhTest {

    @Test
    fun `toKWh divides by 1000`() {
        assertEquals(KWh(2.5), Wh(2500.0).toKWh())
    }

    @Test
    fun `toKWh zero`() {
        assertEquals(KWh(0.0), Wh(0.0).toKWh())
    }

    @Test
    fun `plus Wh`() {
        assertEquals(Wh(500.0), Wh(200.0) + Wh(300.0))
    }

    @Test
    fun `plus KWh converts to Wh then adds`() {
        assertEquals(Wh(1500.0), Wh(500.0) + KWh(1.0))
    }

    @Test
    fun `minus Wh`() {
        assertEquals(Wh(100.0), Wh(400.0) - Wh(300.0))
    }

    @Test
    fun `minus KWh converts to Wh then subtracts`() {
        assertEquals(Wh(500.0), Wh(1500.0) - KWh(1.0))
    }

    @Test
    fun `toKWh and back roundtrip`() {
        val original = Wh(5678.0)
        assertEquals(original, original.toKWh().toWh())
    }
}
