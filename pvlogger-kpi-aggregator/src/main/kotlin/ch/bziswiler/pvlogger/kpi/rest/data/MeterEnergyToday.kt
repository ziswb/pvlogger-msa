package ch.bziswiler.pvlogger.kpi.rest.data

import kotlinx.serialization.Serializable

@Serializable
data class MeterEnergyToday(val imported: Double, val exported: Double)