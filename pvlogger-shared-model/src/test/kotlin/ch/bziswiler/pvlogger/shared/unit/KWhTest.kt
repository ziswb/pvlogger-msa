package ch.bziswiler.pvlogger.shared.unit

import kotlin.test.Test
import kotlin.test.assertEquals

class KWhTest {

    @Test
    fun `toWh multiplies by 1000`() {
        assertEquals(Wh(2500.0), KWh(2.5).toWh())
    }

    @Test
    fun `toWh zero`() {
        assertEquals(Wh(0.0), KWh(0.0).toWh())
    }

    @Test
    fun `plus KWh`() {
        assertEquals(KWh(5.0), KWh(2.0) + KWh(3.0))
    }

    @Test
    fun `plus Wh converts to KWh then adds`() {
        assertEquals(KWh(2.5), KWh(2.0) + Wh(500.0))
    }

    @Test
    fun `minus KWh`() {
        assertEquals(KWh(1.0), KWh(3.0) - KWh(2.0))
    }

    @Test
    fun `minus Wh converts to KWh then subtracts`() {
        assertEquals(KWh(2.0), KWh(2.5) - Wh(500.0))
    }

    @Test
    fun `toWh and back roundtrip`() {
        val original = KWh(7.89)
        assertEquals(original, original.toWh().toKWh())
    }
}
