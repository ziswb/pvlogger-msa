package ch.bziswiler.pvlogger.kpi.rest

data class InverterActivePower(private val power: Double) {
    val producing: Double
        get() = power
}
