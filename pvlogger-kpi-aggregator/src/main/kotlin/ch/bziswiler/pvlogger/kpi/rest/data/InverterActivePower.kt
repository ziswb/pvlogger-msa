package ch.bziswiler.pvlogger.kpi.rest.data

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import ch.bziswiler.pvlogger.shared.masterdata.modbus.toW
import kotlinx.serialization.Serializable

@Serializable
data class InverterActivePower(private val producingValue: Double, private val sourceUnit: Unit) {
    val producing
        get() = sourceUnit.toW(producingValue)
}