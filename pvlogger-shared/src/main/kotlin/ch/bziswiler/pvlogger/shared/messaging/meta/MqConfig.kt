package ch.bziswiler.pvlogger.shared.messaging.meta

data class MqConfig(val clientId: String, val user: String, val passwd: String, val brokerUrl: String)
