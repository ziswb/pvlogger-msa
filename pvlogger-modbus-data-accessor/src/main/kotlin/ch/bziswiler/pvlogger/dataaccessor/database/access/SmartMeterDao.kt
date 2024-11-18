package ch.bziswiler.pvlogger.dataaccessor.database.access

class SmartMeterDao(deviceId: Int) : DeviceDao(deviceId) {

    private val activePowerRegisterAddress = 37113 // -> W
    private val wToKw = 1000.0

    override fun activePowerKW(): Double = readAndTransposeLatestRegisterValue(activePowerRegisterAddress, wToKw)
}
