package ch.bziswiler.pvlogger.shared.masterdata.modbus

import java.io.Serializable

@kotlinx.serialization.Serializable
data class RegisterAddress(val value: Int) : Comparable<RegisterAddress>, Serializable {
    override fun compareTo(other: RegisterAddress): Int = this.value.compareTo(other.value)
}
