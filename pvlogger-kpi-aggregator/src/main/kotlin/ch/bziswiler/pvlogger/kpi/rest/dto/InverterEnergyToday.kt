package ch.bziswiler.pvlogger.kpi.rest.dto

import kotlinx.serialization.Serializable

@Serializable
data class InverterEnergyToday(val produced: Double)
