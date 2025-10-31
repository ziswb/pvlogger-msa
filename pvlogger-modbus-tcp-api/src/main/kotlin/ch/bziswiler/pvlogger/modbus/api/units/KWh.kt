package ch.bziswiler.pvlogger.modbus.api.units

import kotlinx.serialization.Serializable

@Serializable
data class KWh(val kWh: Double) {

    fun toWh() = Wh(kWh.times(1000.0))

    operator fun plus(other: KWh) = KWh(this.kWh + other.kWh)
    operator fun plus(other: Wh) = this + other.toKWh()

    operator fun minus(other: KWh) = KWh(this.kWh - other.kWh)
    operator fun minus(other: Wh) = this - other.toKWh()
}
