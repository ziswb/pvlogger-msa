package ch.bziswiler.pvlogger.kpi.rest

import kotlinx.serialization.Serializable

@Serializable
data class InverterActivePower(val producing: Double)