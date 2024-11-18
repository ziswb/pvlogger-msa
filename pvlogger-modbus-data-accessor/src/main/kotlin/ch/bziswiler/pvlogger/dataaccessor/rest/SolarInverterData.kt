package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.shared.masterdata.unit.KW
import ch.bziswiler.pvlogger.shared.masterdata.unit.KWh

data class SolarInverterData(val activePowerKw: KW, val energyYieldKwh: KWh)
