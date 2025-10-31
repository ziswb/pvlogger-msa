package ch.bziswiler.pvlogger.modbus.api.masterdata

import kotlinx.serialization.Serializable

@Serializable
data class RegisterAddress(val value: Int) : Comparable<RegisterAddress> {
    override fun compareTo(other: RegisterAddress): Int = this.value.compareTo(other.value)
}
