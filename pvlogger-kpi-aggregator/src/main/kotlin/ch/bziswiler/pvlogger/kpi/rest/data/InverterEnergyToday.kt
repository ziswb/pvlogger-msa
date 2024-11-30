package ch.bziswiler.pvlogger.kpi.rest.data

import kotlinx.serialization.Serializable

@Serializable
data class InverterEnergyToday(val produced: Double)
