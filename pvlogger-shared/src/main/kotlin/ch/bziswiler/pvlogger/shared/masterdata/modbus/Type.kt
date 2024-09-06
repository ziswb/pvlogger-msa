package ch.bziswiler.pvlogger.shared.masterdata.modbus

enum class Type(val raw: List<String>, val description: String) {
    U16(listOf("U16", "UINT16"), "Unsigned integer (16 bits)"),
    U32(listOf("U32", "UINT32"), "Unsigned integer (32 bits)"),
    I16(listOf("I16", "INT16"), "Signed integer (16 bits)"),
    I32(listOf("I32", "INT32"), "Signed integer (32 bits)"),
    STR(listOf("STR"), "Character string"),
    MLD(listOf("MLD"), "Multiple data"),
    BITFIELD_16(listOf("BITFIELD_16", "BITFIELD16"), "16-bit data expressed by bit"),
    BITFIELD_32(listOf("BITFIELD_32", "BITFIELD32"), "32-bit data expressed by bit"),
    ;

    companion object {
        fun from(s: String): Type = entries.find { it.name == s.uppercase() || it.raw.contains(s.uppercase()) }!!
    }
}
