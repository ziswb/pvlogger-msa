package ch.bziswiler.pvlogger.shared.masterdata.modbus

import java.io.Serializable

@kotlinx.serialization.Serializable
data class Quantity(val value: Int) : Serializable
