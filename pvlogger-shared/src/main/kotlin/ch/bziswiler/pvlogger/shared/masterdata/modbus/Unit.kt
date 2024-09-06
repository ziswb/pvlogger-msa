package ch.bziswiler.pvlogger.shared.masterdata.modbus

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
