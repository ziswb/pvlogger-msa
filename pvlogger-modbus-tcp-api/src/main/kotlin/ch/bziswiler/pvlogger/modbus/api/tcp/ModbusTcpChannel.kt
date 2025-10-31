package ch.bziswiler.pvlogger.modbus.api.tcp

interface ModbusTcpChannel {
    suspend fun request(request: ModbusRequest): ModbusResponse
}