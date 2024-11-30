package ch.bziswiler.pvlogger.kpi.rest.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeterEnergyToday(val imported: Double, val exported: Double)