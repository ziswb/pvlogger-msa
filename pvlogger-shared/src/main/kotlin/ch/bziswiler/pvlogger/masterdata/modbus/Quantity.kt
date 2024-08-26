package ch.bziswiler.pvlogger.masterdata.modbus

import java.io.Serializable

@kotlinx.serialization.Serializable
data class Quantity(val value: Int) : Serializable
