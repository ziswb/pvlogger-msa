package ch.bziswiler.pvlogger.kpi.rest

import kotlinx.serialization.Serializable

@Serializable
data class InverterEnergyToday(val produced: Double)
