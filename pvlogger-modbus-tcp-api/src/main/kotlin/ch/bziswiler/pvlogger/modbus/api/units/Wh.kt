package ch.bziswiler.pvlogger.modbus.api.units

import kotlinx.serialization.Serializable

@Serializable
data class Wh(val wh: Double) {

    fun toKWh() = KWh(wh.div(1000.0))

    operator fun plus(other: Wh) = Wh(this.wh + other.wh)
    operator fun plus(other: KWh) = this + other.toWh()

    operator fun minus(other: Wh) = Wh(this.wh - other.wh)
    operator fun minus(other: KWh) = this - other.toWh()
}
