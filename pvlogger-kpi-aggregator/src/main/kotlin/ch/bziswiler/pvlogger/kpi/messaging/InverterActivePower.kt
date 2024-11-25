package ch.bziswiler.pvlogger.kpi.messaging

data class InverterActivePower(private val power: Double) {
    val producing: Double
        get() = power
}
