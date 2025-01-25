package ch.bziswiler.pvlogger.shared.unit

import kotlinx.serialization.Serializable

@Serializable
data class W(val w: Double) {

    fun toKW() = KW(w.div(1000))

    operator fun plus(other: W) = W(this.w + other.w)
    operator fun plus(other: KW) = this + other.toW()

    operator fun minus(other: W) = W(this.w - other.w)
    operator fun minus(other: KW) = this - other.toW()
}
