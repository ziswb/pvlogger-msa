package ch.bziswiler.pvlogger.kpi.rest.dto

import kotlinx.serialization.Serializable

@Serializable
data class InverterActivePower(val producing: Double)