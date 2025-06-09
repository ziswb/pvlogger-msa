package ch.bziswiler.pvlogger.modbus.tcp.api.masterdata

import ch.bziswiler.pvlogger.shared.unit.KW
import ch.bziswiler.pvlogger.shared.unit.KWh
import ch.bziswiler.pvlogger.shared.unit.W

enum class Unit(val raw: List<String>) {
    PERCENT(listOf("%")),
    PERCENT_PER_SECOND(listOf("%/s")),
    AMPERE(listOf("A")),
    HERZ(listOf("Hz")),
    K_VA(listOf("kVA")),
    K_VAR(listOf("kVar")),
    K_VAR_H(listOf("kVar h")),
    VAR(listOf("Var")),
    KW(listOf("kW")),
    KWH(listOf("kWh")),
    MIN(listOf("min")),
    NA(listOf("N/A", "NA")),
    S(listOf("S", "s")),
    V(listOf("V")),
    W(listOf("W")),
    C(listOf("°C")),
    MO(listOf("MΩ")),
    ;

    companion object {
        fun from(s: String): Unit = entries.find { it.raw.contains(s.trim()) }!!
    }
}

fun Unit.toW(value: Double): W {
    return when (this) {
        Unit.W -> W(value)
        Unit.KW -> KW(value).toW()
        else -> throw IllegalStateException("unit $this is not a known power unit")
    }
}

fun Unit.toKWh(value: Double): KWh {
    return when (this) {
        Unit.KWH -> KWh(value)
        else -> throw IllegalStateException("unit $this is not a known energy unit")
    }
}
