package ch.bziswiler.pvlogger.modbus.tcp.api.frame

class MbapHeader(
    var protocolIdentifier: UShort,
    var protocolType: UShort = 0x0u,
    var dataLength: UShort,
    var logicDeviceId: UByte,
)
