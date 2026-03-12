package ch.bziswiler.pvlogger.shared.masterdata.modbus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnumParserTest {

    // --- Gain.from ---

    @Test
    fun `Gain from valid raw values`() {
        assertEquals(Gain.ONE, Gain.from("1"))
        assertEquals(Gain.TEN, Gain.from("10"))
        assertEquals(Gain.HUNDRED, Gain.from("100"))
        assertEquals(Gain.THOUSAND, Gain.from("1000"))
        assertEquals(Gain.ASTERISK, Gain.from("*"))
    }

    @Test
    fun `Gain from unknown value throws`() {
        assertFailsWith<NullPointerException> { Gain.from("unknown") }
    }

    // --- Type.from ---

    @Test
    fun `Type from primary names`() {
        assertEquals(Type.U16, Type.from("U16"))
        assertEquals(Type.U32, Type.from("U32"))
        assertEquals(Type.I16, Type.from("I16"))
        assertEquals(Type.I32, Type.from("I32"))
        assertEquals(Type.STR, Type.from("STR"))
        assertEquals(Type.MLD, Type.from("MLD"))
        assertEquals(Type.BITFIELD_16, Type.from("BITFIELD_16"))
        assertEquals(Type.BITFIELD_32, Type.from("BITFIELD_32"))
    }

    @Test
    fun `Type from alias names`() {
        assertEquals(Type.U16, Type.from("UINT16"))
        assertEquals(Type.U32, Type.from("UINT32"))
        assertEquals(Type.I16, Type.from("INT16"))
        assertEquals(Type.I32, Type.from("INT32"))
        assertEquals(Type.BITFIELD_16, Type.from("BITFIELD16"))
        assertEquals(Type.BITFIELD_32, Type.from("BITFIELD32"))
    }

    @Test
    fun `Type from is case insensitive`() {
        assertEquals(Type.U16, Type.from("u16"))
        assertEquals(Type.STR, Type.from("str"))
        assertEquals(Type.I32, Type.from("i32"))
    }

    @Test
    fun `Type from unknown value throws`() {
        assertFailsWith<NullPointerException> { Type.from("UNKNOWN") }
    }

    // --- Unit.from ---

    @Test
    fun `Unit from valid raw values`() {
        assertEquals(Unit.PERCENT, Unit.from("%"))
        assertEquals(Unit.AMPERE, Unit.from("A"))
        assertEquals(Unit.HERZ, Unit.from("Hz"))
        assertEquals(Unit.KW, Unit.from("kW"))
        assertEquals(Unit.KWH, Unit.from("kWh"))
        assertEquals(Unit.V, Unit.from("V"))
        assertEquals(Unit.W, Unit.from("W"))
        assertEquals(Unit.C, Unit.from("°C"))
        assertEquals(Unit.MO, Unit.from("MΩ"))
    }

    @Test
    fun `Unit from NA aliases`() {
        assertEquals(Unit.NA, Unit.from("N/A"))
        assertEquals(Unit.NA, Unit.from("NA"))
    }

    @Test
    fun `Unit from S aliases`() {
        assertEquals(Unit.S, Unit.from("S"))
        assertEquals(Unit.S, Unit.from("s"))
    }

    @Test
    fun `Unit from trims whitespace`() {
        assertEquals(Unit.W, Unit.from(" W "))
        assertEquals(Unit.KW, Unit.from("  kW  "))
    }

    @Test
    fun `Unit from unknown value throws`() {
        assertFailsWith<NullPointerException> { Unit.from("unknown") }
    }
}
