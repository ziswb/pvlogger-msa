package ch.bziswiler.pvlogger.kpi.rest

data class InverterEnergyToday(private val power: Double) {
    val produced: Double
        get() = power
}
