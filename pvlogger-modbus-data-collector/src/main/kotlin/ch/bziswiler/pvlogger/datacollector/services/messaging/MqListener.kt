package ch.bziswiler.pvlogger.datacollector.services.messaging

interface MqListener {

    val messageSelector: String

    fun messageReceived()
}