package ch.bziswiler.pvlogger.shared.unit

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class KWh(val kWh: Double) : JavaSerializable {

    fun toWh() = Wh(kWh.times(1000))

    operator fun plus(other: KWh) = KWh(this.kWh + other.kWh)
    operator fun plus(other: Wh) = this + other.toKWh()

    operator fun minus(other: KWh) = KWh(this.kWh - other.kWh)
    operator fun minus(other: Wh) = this - other.toKWh()
}
