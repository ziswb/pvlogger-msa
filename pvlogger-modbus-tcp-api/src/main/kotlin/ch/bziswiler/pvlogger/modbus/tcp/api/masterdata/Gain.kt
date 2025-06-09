package ch.bziswiler.pvlogger.modbus.tcp.api.masterdata

enum class Gain(val raw: String) {
    ONE("1"),
    TEN("10"),
    HUNDRED("100"),
    THOUSAND("1000"),
    ASTERISK("*"),
    ;

    companion object {
        fun from(s: String): Gain = entries.find { it.raw == s }!!
    }
}
