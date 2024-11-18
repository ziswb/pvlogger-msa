package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Gain
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress

class SmartMeterDao(deviceId: Int, activePowerRegisterAddress: RegisterAddress) : SolarDeviceDao(deviceId, activePowerRegisterAddress) {

    //    private val smartMeterActivePowerRegisterAddress = 37113 // -> W
    override val activePowerGain = Gain.THOUSAND
}
