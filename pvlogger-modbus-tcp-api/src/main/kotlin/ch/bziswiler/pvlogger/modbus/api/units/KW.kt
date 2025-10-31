package ch.bziswiler.pvlogger.modbus.api.units

import kotlinx.serialization.Serializable

@Serializable
data class KW(val kw: Double) {

    fun toW() = W(kw.times(1000.0))

    operator fun plus(other: KW) = KW(this.kw + other.kw)
    operator fun plus(other: W) = this + other.toKW()

    operator fun minus(other: KW) = KW(this.kw - other.kw)
    operator fun minus(other: W) = this - other.toKW()
}
