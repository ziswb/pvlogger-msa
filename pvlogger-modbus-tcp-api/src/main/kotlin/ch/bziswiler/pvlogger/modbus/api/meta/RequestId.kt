package ch.bziswiler.pvlogger.modbus.api.meta

import kotlinx.serialization.Serializable

@Serializable
data class RequestId(val id: UShort)
