package ch.bziswiler.pvlogger.shared.masterdata.unit

import kotlinx.serialization.Serializable

@Serializable
data class W(val value: Double) {
    fun kW(): Wh = Wh(value.div(1000))
    operator fun plus(that: W): W = W(value + that.value)
    operator fun plus(that: KW): W = W(value + that.w().value)
}

@Serializable
data class KW(val value: Double) {
    fun w(): W = W(value.times(1000))
    operator fun plus(that: KW): KW = KW(value + that.value)
    operator fun plus(that: W): KW = KW(value + that.kW().value)
}

@Serializable
data class Wh(val value: Double) {
    fun kWh(): KWh = KWh(value.div(1000))
    operator fun plus(that: Wh): Wh = Wh(value + that.value)
    operator fun plus(that: KWh): Wh = Wh(value + that.wh().value)
}

@Serializable
data class KWh(val value: Double) {
    fun wh(): Wh = Wh(value.times(1000))
    operator fun plus(that: KWh): KWh = KWh(value + that.value)
    operator fun plus(that: Wh): KWh = KWh(value + that.kWh().value)
}
