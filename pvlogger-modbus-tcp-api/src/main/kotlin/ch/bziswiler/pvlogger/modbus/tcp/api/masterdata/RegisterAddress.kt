package ch.bziswiler.pvlogger.modbus.tcp.api.masterdata

data class RegisterAddress(val value: Int) : Comparable<RegisterAddress> {
    override fun compareTo(other: RegisterAddress): Int = this.value.compareTo(other.value)
}
