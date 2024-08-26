package ch.bziswiler.pvlogger.masterdata.modbus

enum class ReadWrite(val description: String) {
    RO("Data that is readable only"),
    RW("Data that is readable and writable"),
    WO("Data that is writable only"),
}
