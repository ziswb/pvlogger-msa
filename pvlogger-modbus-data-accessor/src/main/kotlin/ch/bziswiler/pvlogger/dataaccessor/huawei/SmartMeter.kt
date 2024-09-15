package ch.bziswiler.pvlogger.business.model.huawei.device

class SmartMeter(deviceId: Int) : Device(deviceId) {

    private val activePowerRegisterAddress = 37113 // -> W
    private val wToKw = 1000.0

    override fun activePowerKW(): Double = activePowerKW(activePowerRegisterAddress, wToKw)
}
