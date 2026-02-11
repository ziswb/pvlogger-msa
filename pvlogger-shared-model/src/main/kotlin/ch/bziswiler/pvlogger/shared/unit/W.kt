package ch.bziswiler.pvlogger.shared.unit

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class W(val w: Double) : JavaSerializable {

    fun toKW() = KW(w.div(1000.0))

    operator fun plus(other: W) = W(this.w + other.w)
    operator fun plus(other: KW) = this + other.toW()

    operator fun minus(other: W) = W(this.w - other.w)
    operator fun minus(other: KW) = this - other.toW()
}
