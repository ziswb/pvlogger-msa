package ch.bziswiler.pvlogger.masterdata.modbus

import java.io.Serializable

@kotlinx.serialization.Serializable
data class Scope(val value: String) : Serializable
