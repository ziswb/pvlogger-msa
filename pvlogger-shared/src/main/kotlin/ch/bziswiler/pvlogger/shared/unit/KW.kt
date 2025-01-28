package ch.bziswiler.pvlogger.shared.unit

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class KW(val kw: Double) : JavaSerializable {

    fun toW() = W(kw.times(1000))

    operator fun plus(other: KW) = KW(this.kw + other.kw)
    operator fun plus(other: W) = this + other.toKW()

    operator fun minus(other: KW) = KW(this.kw - other.kw)
    operator fun minus(other: W) = this - other.toKW()
}
